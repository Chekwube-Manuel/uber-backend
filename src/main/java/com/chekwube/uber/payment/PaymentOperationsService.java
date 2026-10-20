package com.chekwube.uber.payment;

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
import java.util.UUID;

@Service
public class PaymentOperationsService {
    private final PaymentRepository payments;
    private final PaymentRefundRepository refunds;
    private final DriverPayoutRepository payouts;
    private final RideRepository rides;
    private final PaystackClient paystack;
    private final TransactionTemplate transactions;
    private final BigDecimal platformCommissionPercent;

    public PaymentOperationsService(PaymentRepository payments, PaymentRefundRepository refunds,
                                    DriverPayoutRepository payouts, RideRepository rides, PaystackClient paystack,
                                    PlatformTransactionManager transactionManager,
                                    @Value("${PLATFORM_COMMISSION_PERCENT:20}") BigDecimal platformCommissionPercent) {
        this.payments = payments; this.refunds = refunds; this.payouts = payouts; this.rides = rides;
        this.paystack = paystack; this.transactions = new TransactionTemplate(transactionManager);
        if (platformCommissionPercent.signum() < 0 || platformCommissionPercent.compareTo(BigDecimal.valueOf(100)) >= 0)
            throw new IllegalArgumentException("PLATFORM_COMMISSION_PERCENT must be >= 0 and < 100");
        this.platformCommissionPercent = platformCommissionPercent;
    }

    public RefundResponse refund(String paymentReference, String reason) {
        RefundPreparation prep = transactions.execute(tx -> {
            Payment payment = payments.findByReferenceForUpdate(paymentReference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
            if (payment.getStatus() != PaymentStatus.SUCCESS)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Only successful payments can be refunded");
            if (refunds.existsByPaymentIdAndStatusIn(payment.getId(),
                java.util.List.of("INITIALIZED", "PENDING", "PROCESSING", "NEEDS_ATTENTION", "PROCESSED")))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A refund already exists for this payment");
            String refundReference = "ride-refund-" + UUID.randomUUID();
            PaymentRefund refund = refunds.saveAndFlush(new PaymentRefund(payment.getId(), refundReference, payment.getAmountMinor(), reason));
            payment.markRefundPending(); payments.save(payment);
            return new RefundPreparation(refund.getId(), payment.getProviderReference(), refund.getAmountMinor());
        });
        PaystackClient.ProviderOperation provider = paystack.refund(prep.paymentReference(), reason);
        return transactions.execute(tx -> {
            PaymentRefund refund = refunds.findById(prep.refundId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Refund record not found"));
            String status = normalizeRefundStatus(provider.status());
            refund.update(status, provider.providerId()); refunds.save(refund);
            Payment payment = payments.findByProviderReference(prep.paymentReference()).orElseThrow();
            if ("PROCESSED".equals(status)) payment.markRefunded();
            payments.save(payment);
            return new RefundResponse(refund.getId(), prep.paymentReference(), refund.getAmountMinor(), refund.getStatus());
        });
    }

    public PayoutResponse payout(UUID rideId, String recipientCode) {
        PayoutPreparation prep = transactions.execute(tx -> {
            Ride ride = rides.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
            if (ride.getStatus() != RideStatus.COMPLETED || ride.getDriverUserId() == null)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Only completed rides assigned to a driver can be paid out");
            Payment payment = payments.findByRideIdAndStatus(rideId, PaymentStatus.SUCCESS)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Ride must have a successful rider payment first"));
            if (payouts.findByRideId(rideId).isPresent())
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A payout already exists for this ride");
            long amount = BigDecimal.valueOf(payment.getAmountMinor())
                .multiply(BigDecimal.valueOf(100).subtract(platformCommissionPercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN).longValueExact();
            if (amount <= 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Calculated driver payout is zero");
            String reference = "ride-payout-" + UUID.randomUUID();
            DriverPayout payout = payouts.saveAndFlush(new DriverPayout(rideId, payment.getId(), ride.getDriverUserId(),
                recipientCode, reference, amount));
            return new PayoutPreparation(payout.getId(), reference, amount, ride.getDriverUserId());
        });
        PaystackClient.ProviderOperation provider = paystack.transfer(recipientCode, prep.amountMinor(),
            prep.reference(), "Driver payout for ride " + rideId);
        return transactions.execute(tx -> {
            DriverPayout payout = payouts.findById(prep.payoutId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout record not found"));
            payout.update(normalizeTransferStatus(provider.status()), provider.code());
            payouts.save(payout);
            return new PayoutResponse(payout.getId(), payout.getRideId(), payout.getDriverUserId(),
                payout.getAmountMinor(), payout.getCurrency(), payout.getStatus(), payout.getProviderReference());
        });
    }

    public void handleRefundWebhook(String paymentReference, String event, Long providerRefundId) {
        transactions.executeWithoutResult(tx -> {
            Payment payment = payments.findByProviderReference(paymentReference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
            PaymentRefund refund = refunds.findTopByPaymentIdOrderByCreatedAtDesc(payment.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Refund record not found"));
            String status = normalizeRefundStatus(event.startsWith("refund.") ? event.substring(7) : event);
            refund.update(status, providerRefundId); refunds.save(refund);
            if ("PROCESSED".equals(status)) payment.markRefunded();
            else if ("FAILED".equals(status)) payment.markSuccessful(payment.getProviderTransactionId());
            payments.save(payment);
        });
    }

    public void handleTransferWebhook(String reference, String event, String transferCode) {
        transactions.executeWithoutResult(tx -> {
            DriverPayout payout = payouts.findByProviderReference(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout reference not found"));
            payout.update(normalizeTransferStatus(event.startsWith("transfer.") ? event.substring(9) : event), transferCode);
            payouts.save(payout);
        });
    }

    private String normalizeRefundStatus(String status) {
        return switch (status.toLowerCase(java.util.Locale.ROOT).replace('-', '_')) {
            case "processed", "success" -> "PROCESSED";
            case "processing" -> "PROCESSING";
            case "failed" -> "FAILED";
            case "needs_attention" -> "NEEDS_ATTENTION";
            default -> "PENDING";
        };
    }
    private String normalizeTransferStatus(String status) {
        return switch (status.toLowerCase(java.util.Locale.ROOT)) {
            case "success", "successful" -> "SUCCESS";
            case "failed", "reversed" -> "FAILED";
            default -> "PENDING";
        };
    }
    private record RefundPreparation(UUID refundId, String paymentReference, long amountMinor) {}
    private record PayoutPreparation(UUID payoutId, String reference, long amountMinor, UUID driverUserId) {}
    public record RefundResponse(UUID refundId, String paymentReference, long amountMinor, String status) {}
    public record PayoutResponse(UUID payoutId, UUID rideId, UUID driverUserId, long amountMinor,
                                 String currency, String status, String reference) {}
}
