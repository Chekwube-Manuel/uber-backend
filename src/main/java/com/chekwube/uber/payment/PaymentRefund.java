package com.chekwube.uber.payment;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_refunds")
public class PaymentRefund {
    @Id private UUID id;
    @Column(name = "payment_id", nullable = false) private UUID paymentId;
    @Column(name = "provider_reference", nullable = false, unique = true, length = 100) private String providerReference;
    @Column(name = "provider_refund_id") private Long providerRefundId;
    @Column(name = "amount_minor", nullable = false) private long amountMinor;
    @Column(nullable = false, length = 24) private String status;
    @Column(nullable = false, length = 500) private String reason;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected PaymentRefund() {}
    public PaymentRefund(UUID paymentId, String reference, long amountMinor, String reason) {
        this.id = UUID.randomUUID(); this.paymentId = paymentId; this.providerReference = reference;
        this.amountMinor = amountMinor; this.reason = reason; this.status = "INITIALIZED";
        this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
    }
    public void update(String status, Long providerRefundId) {
        this.status = status;
        if (providerRefundId != null) this.providerRefundId = providerRefundId;
        this.updatedAt = Instant.now();
    }
    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public String getProviderReference() { return providerReference; }
    public Long getProviderRefundId() { return providerRefundId; }
    public long getAmountMinor() { return amountMinor; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
