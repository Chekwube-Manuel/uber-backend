package com.chekwube.uber.payment;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {
    @Id private UUID id;
    @Column(name = "ride_id", nullable = false) private UUID rideId;
    @Column(name = "rider_user_id", nullable = false) private UUID riderUserId;
    @Column(nullable = false, length = 24) private String provider;
    @Column(name = "provider_reference", nullable = false, unique = true, length = 100) private String providerReference;
    @Column(name = "amount_minor", nullable = false) private long amountMinor;
    @Column(nullable = false, length = 3) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentStatus status;
    @Column(name = "authorization_url", columnDefinition = "text") private String authorizationUrl;
    @Column(name = "access_code", length = 128) private String accessCode;
    @Column(name = "provider_transaction_id") private Long providerTransactionId;
    @Column(name = "paid_at") private Instant paidAt;
    @Column(name = "dispute_reference", length = 100) private String disputeReference;
    @Column(name = "dispute_status", length = 40) private String disputeStatus;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Payment() {}
    public Payment(UUID rideId, UUID riderUserId, String providerReference, long amountMinor) {
        this.id = UUID.randomUUID(); this.rideId = rideId; this.riderUserId = riderUserId;
        this.provider = "PAYSTACK"; this.providerReference = providerReference; this.amountMinor = amountMinor;
        this.currency = "NGN"; this.status = PaymentStatus.INITIALIZED;
        this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
    }
    public void setCheckout(String authorizationUrl, String accessCode) {
        if (this.status == PaymentStatus.SUCCESS || this.status == PaymentStatus.REFUNDED || this.status == PaymentStatus.CHARGEBACK) return;
        this.authorizationUrl = authorizationUrl; this.accessCode = accessCode;
        this.status = PaymentStatus.PENDING; this.updatedAt = Instant.now();
    }
    public void markSuccessful(Long transactionId) {
        if (this.status == PaymentStatus.REFUNDED || this.status == PaymentStatus.CHARGEBACK) return;
        this.status = PaymentStatus.SUCCESS; this.providerTransactionId = transactionId;
        this.paidAt = Instant.now(); this.updatedAt = this.paidAt;
    }
    public void markFailed(Long transactionId) {
        if (this.status == PaymentStatus.SUCCESS || this.status == PaymentStatus.REFUNDED || this.status == PaymentStatus.CHARGEBACK) return;
        this.status = PaymentStatus.FAILED;
        if (transactionId != null) this.providerTransactionId = transactionId;
        this.updatedAt = Instant.now();
    }
    public void markRefundPending() { this.status = PaymentStatus.REFUND_PENDING; this.updatedAt = Instant.now(); }
    public void markRefunded() { this.status = PaymentStatus.REFUNDED; this.updatedAt = Instant.now(); }
    public void markChargeback(String disputeReference, String disputeStatus) {
        this.disputeReference = disputeReference; this.disputeStatus = disputeStatus;
        this.status = PaymentStatus.CHARGEBACK; this.updatedAt = Instant.now();
    }
    public UUID getId() { return id; }
    public UUID getRideId() { return rideId; }
    public UUID getRiderUserId() { return riderUserId; }
    public String getProvider() { return provider; }
    public String getProviderReference() { return providerReference; }
    public long getAmountMinor() { return amountMinor; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public String getAuthorizationUrl() { return authorizationUrl; }
    public String getAccessCode() { return accessCode; }
    public Long getProviderTransactionId() { return providerTransactionId; }
    public Instant getPaidAt() { return paidAt; }
    public String getDisputeReference() { return disputeReference; }
    public String getDisputeStatus() { return disputeStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
