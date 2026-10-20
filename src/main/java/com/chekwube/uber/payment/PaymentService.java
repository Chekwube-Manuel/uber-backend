package com.chekwube.uber.payment;

import com.chekwube.uber.auth.AppUser;
import com.chekwube.uber.auth.UserRepository;
import com.chekwube.uber.ride.Ride;
import com.chekwube.uber.ride.RideRepository;
import com.chekwube.uber.ride.RideStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final RideRepository rides;
    private final UserRepository users;
    private final PaystackClient paystack;
    private final String callbackUrl;

    public PaymentService(PaymentRepository payments, RideRepository rides, UserRepository users,
                          PaystackClient paystack,
                          @Value("${PAYSTACK_CALLBACK_URL:}") String callbackUrl) {
        this.payments = payments;
        this.rides = rides;
        this.users = users;
        this.paystack = paystack;
        this.callbackUrl = callbackUrl;
    }

    @Transactional
    public PaymentDtos.CheckoutResponse initialize(UUID riderId, UUID rideId) {
        Ride ride = rides.findByIdForUpdate(rideId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (!ride.getRiderUserId().equals(riderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found");
        }
        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment is available only after a ride is completed");
        }
        if (payments.existsByRideIdAndStatus(rideId, PaymentStatus.SUCCESS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This ride has already been paid");
        }

        Payment latest = payments.findTopByRideIdAndRiderUserIdOrderByCreatedAtDesc(rideId, riderId).orElse(null);
        if (latest != null && latest.getStatus() == PaymentStatus.PENDING
            && latest.getAuthorizationUrl() != null && !latest.getAuthorizationUrl().isBlank()) {
            return PaymentDtos.CheckoutResponse.from(latest);
        }

        BigDecimal fare = ride.getFinalFareNaira();
        if (fare == null || fare.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ride has no payable final fare");
        }
        long amountMinor;
        try {
            amountMinor = fare.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ride fare is invalid for payment");
        }

        AppUser rider = users.findById(riderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rider not found"));
        String reference = "ridepay-" + UUID.randomUUID();
        Payment payment = payments.saveAndFlush(new Payment(rideId, riderId, reference, amountMinor));
        PaystackClient.Checkout checkout = paystack.initialize(rider.getEmail(), amountMinor, reference, callbackUrl);
        payment.setCheckout(checkout.authorizationUrl(), checkout.accessCode());
        return PaymentDtos.CheckoutResponse.from(payments.save(payment));
    }

    @Transactional
    public PaymentDtos.PaymentResponse verify(UUID riderId, String reference) {
        Payment payment = payments.findByReferenceForUpdate(reference)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (!payment.getRiderUserId().equals(riderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) return PaymentDtos.PaymentResponse.from(payment);

        PaystackClient.VerifiedTransaction verified = paystack.verify(reference);
        applyVerified(payment, verified);
        return PaymentDtos.PaymentResponse.from(payments.save(payment));
    }

    @Transactional
    public void handleSuccessfulWebhook(String reference, long amountMinor, String currency, Long transactionId) {
        Payment payment = payments.findByReferenceForUpdate(reference)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment reference not found"));
        if (payment.getAmountMinor() != amountMinor || !payment.getCurrency().equalsIgnoreCase(currency)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook amount or currency does not match payment");
        }
        payment.markSuccessful(transactionId);
        payments.save(payment);
    }

    private void applyVerified(Payment payment, PaystackClient.VerifiedTransaction verified) {
        if (!payment.getProviderReference().equals(verified.reference())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Paystack reference mismatch");
        }
        if (payment.getAmountMinor() != verified.amountMinor()
            || !payment.getCurrency().equalsIgnoreCase(verified.currency())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Verified amount or currency does not match payment");
        }
        if ("success".equalsIgnoreCase(verified.status())) {
            payment.markSuccessful(verified.transactionId());
        } else if ("failed".equalsIgnoreCase(verified.status()) || "abandoned".equalsIgnoreCase(verified.status())
            || "reversed".equalsIgnoreCase(verified.status())) {
            payment.markFailed(verified.transactionId());
        } else {
            payment.setCheckout(payment.getAuthorizationUrl(), payment.getAccessCode());
        }
    }
}