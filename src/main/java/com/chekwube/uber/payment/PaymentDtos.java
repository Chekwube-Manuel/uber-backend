package com.chekwube.uber.payment;

import java.time.Instant;
import java.util.UUID;

public final class PaymentDtos {
    private PaymentDtos() {}

    public record CheckoutResponse(UUID paymentId, UUID rideId, String reference, String currency,
                                   long amountMinor, PaymentStatus status, String authorizationUrl,
                                   String accessCode, Instant createdAt) {
        static CheckoutResponse from(Payment payment) {
            return new CheckoutResponse(payment.getId(), payment.getRideId(), payment.getProviderReference(),
                payment.getCurrency(), payment.getAmountMinor(), payment.getStatus(), payment.getAuthorizationUrl(),
                payment.getAccessCode(), payment.getCreatedAt());
        }
    }

    public record PaymentResponse(UUID id, UUID rideId, String reference, String currency, long amountMinor,
                                  PaymentStatus status, Long providerTransactionId, Instant paidAt,
                                  Instant createdAt, Instant updatedAt) {
        static PaymentResponse from(Payment payment) {
            return new PaymentResponse(payment.getId(), payment.getRideId(), payment.getProviderReference(),
                payment.getCurrency(), payment.getAmountMinor(), payment.getStatus(),
                payment.getProviderTransactionId(), payment.getPaidAt(), payment.getCreatedAt(), payment.getUpdatedAt());
        }
    }
}