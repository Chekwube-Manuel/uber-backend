package com.chekwube.uber.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, UUID> {
    Optional<PaymentRefund> findByProviderReference(String reference);
    Optional<PaymentRefund> findTopByPaymentIdOrderByCreatedAtDesc(UUID paymentId);
    boolean existsByPaymentIdAndStatusIn(UUID paymentId, java.util.Collection<String> statuses);
}
