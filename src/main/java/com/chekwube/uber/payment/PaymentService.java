package com.chekwube.uber.payment;

import com.chekwube.uber.auth.AppUser;
import com.chekwube.uber.auth.UserRepository;
import com.chekwube.uber.ride.Ride;
import com.chekwube.uber.ride.RideRepository;
import com.chekwube.uber.ride.RideStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PaymentService {
    private static final Set<PaymentStatus> RECONCILABLE = Set.of(PaymentStatus.INITIALIZED, PaymentStatus.PENDING);
    private static final Set<PaymentStatus> PAYMENT_TERMINAL_OR_ACTIVE = Set.of(
        PaymentStatus.SUCCESS, PaymentStatus.REFUND_PENDING, PaymentStatus.REFUNDED, PaymentStatus.CHARGEBACK);
    private final PaymentRepository payments;
    private final RideRepository rides;
    private final UserRepository users;
    private final PaystackClient paystack;
    private final TransactionTemplate transactions;
    private final String callbackUrl;

    public PaymentService(PaymentRepository payments, RideRepository rides, UserRepository users,
                          PaystackClient paystack, PlatformTransactionManager transactionManager,
                          @Value("${PAYSTACK_CALLBACK_URL:}") String callbackUrl) {
        this.payments = payments; this.rides = rides; this.users = users; this.paystack = paystack;
        this.transactions = new TransactionTemplate(transactionManager); this.callbackUrl = callbackUrl;
    }

    public PaymentDtos.CheckoutResponse initialize(UUID riderId, UUID rideId) {
        PreparedCheckout prepared = transactions.execute(tx -> {
            Ride ride = rides.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
            if (!ride.getRiderUserId().equals(riderId))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found");
            BigDecimal fare;
            if (ride.getStatus() == RideStatus.COMPLETED) {
                if (payments.existsByRideIdAndStatusIn(rideId, PAYMENT_TERMINAL_OR_ACTIVE))
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "This ride has already been paid");
                fare = ride.getFinalFareNaira();
            } else if (ride.getStatus() == RideStatus.CANCELLED && ride.getCancellationFeeNaira() != null) {
                fare = ride.getCancellationFeeNaira();
                if (fare.signum() == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "No cancellation fee is due");
                if (payments.existsByRideIdAndStatusIn(rideId, PAYMENT_TERMINAL_OR_ACTIVE))
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "This ride has already been paid");
            } else {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment is available only after a completed ride or a fee-bearing cancellation");
            }
            Payment latest = payments.findTopByRideIdAndRiderUserIdOrderByCreatedAtDesc(rideId, riderId).orElse(null);
            if (latest != null && latest.getStatus() == PaymentStatus.PENDING
                && latest.getAuthorizationUrl() != null && !latest.getAuthorizationUrl().isBlank())
                return new PreparedCheckout(PaymentDtos.CheckoutResponse.from(latest), null, null, null, null);
            if (latest != null && latest.getStatus() == PaymentStatus.INITIALIZED)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment initialization is already in progress; retry shortly");
            if (fare == null || fare.signum() <= 0)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ride has no payable final fare");
            long amountMinor;
            try { amountMinor = fare.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact(); }
            catch (ArithmeticException exception) { throw new ResponseStatusException(HttpStatus.CONFLICT, "Ride fare is invalid for payment"); }
            AppUser rider = users.findById(riderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rider not found"));
            String reference = "ridepay-" + UUID.randomUUID();
            Payment payment = payments.saveAndFlush(new Payment(rideId, riderId, reference, amountMinor));
            return new PreparedCheckout(null, payment.getId(), rider.getEmail(), reference, amountMinor);
        });
        if (prepared.response() != null) return prepared.response();

        PaystackClient.Checkout checkout;
        try {
            checkout = paystack.initialize(prepared.email(), prepared.amountMinor(), prepared.reference(), callbackUrl);
        } catch (RuntimeException exception) {
            transactions.executeWithoutResult(tx -> payments.findByReferenceForUpdate(prepared.reference())
                .ifPresent(payment -> { payment.markFailed(null); payments.save(payment); }));
            throw exception;
        }
        return transactions.execute(tx -> {
            Payment payment = payments.findByReferenceForUpdate(prepared.reference())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment initialization disappeared"));
            payment.setCheckout(checkout.authorizationUrl(), checkout.accessCode());
            return PaymentDtos.CheckoutResponse.from(payments.save(payment));
        });
    }

    public PaymentDtos.PaymentResponse verify(UUID riderId, String reference) {
        PaymentSnapshot snapshot = transactions.execute(tx -> {
            Payment payment = payments.findByProviderReferenceAndRiderUserId(reference, riderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
            return new PaymentSnapshot(PaymentDtos.PaymentResponse.from(payment), payment.getStatus());
        });
        if (snapshot.status() == PaymentStatus.SUCCESS || snapshot.status() == PaymentStatus.REFUNDED
            || snapshot.status() == PaymentStatus.CHARGEBACK) return snapshot.response();
        PaystackClient.VerifiedTransaction verified = paystack.verify(reference);
        return applyVerified(reference, verified);
    }

    public PaymentDtos.PaymentResponse applyVerified(String reference, PaystackClient.VerifiedTransaction verified) {
        return transactions.execute(tx -> {
            Payment payment = payments.findByReferenceForUpdate(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
            if (!payment.getProviderReference().equals(verified.reference()))
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Paystack reference mismatch");
            if (payment.getAmountMinor() != verified.amountMinor()
                || !payment.getCurrency().equalsIgnoreCase(verified.currency()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Verified amount or currency does not match payment");
            if ("success".equalsIgnoreCase(verified.status())) payment.markSuccessful(verified.transactionId());
            else if ("failed".equalsIgnoreCase(verified.status()) || "abandoned".equalsIgnoreCase(verified.status())
                || "reversed".equalsIgnoreCase(verified.status())) payment.markFailed(verified.transactionId());
            return PaymentDtos.PaymentResponse.from(payments.save(payment));
        });
    }

    public int reconcileStalePayments() {
        List<String> references = transactions.execute(tx -> payments
            .findTop50ByStatusInAndUpdatedAtBeforeOrderByUpdatedAtAsc(RECONCILABLE, Instant.now().minusSeconds(90))
            .stream().map(Payment::getProviderReference).toList());
        int checked = 0;
        for (String reference : references) {
            try { applyVerified(reference, paystack.verify(reference)); checked++; }
            catch (RuntimeException ignored) {
                // A provider timeout is not proof of failure. Leave the row pending and retry on the next schedule.
            }
        }
        return checked;
    }

    public void handleSuccessfulWebhook(String reference, long amountMinor, String currency, Long transactionId) {
        transactions.executeWithoutResult(tx -> {
            Payment payment = payments.findByReferenceForUpdate(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment reference not found"));
            if (payment.getAmountMinor() != amountMinor || !payment.getCurrency().equalsIgnoreCase(currency))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook amount or currency does not match payment");
            payment.markSuccessful(transactionId);
            payments.save(payment);
        });
    }

    public void handleChargeback(String reference, String disputeReference, String disputeStatus) {
        transactions.executeWithoutResult(tx -> {
            Payment payment = payments.findByReferenceForUpdate(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment reference not found"));
            payment.markChargeback(disputeReference, disputeStatus);
            payments.save(payment);
        });
    }

    private record PreparedCheckout(PaymentDtos.CheckoutResponse response, UUID paymentId, String email,
                                    String reference, Long amountMinor) {}
    private record PaymentSnapshot(PaymentDtos.PaymentResponse response, PaymentStatus status) {}
}
