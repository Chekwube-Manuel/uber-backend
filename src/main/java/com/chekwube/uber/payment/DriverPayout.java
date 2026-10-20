package com.chekwube.uber.payment;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "driver_payouts")
public class DriverPayout {
    @Id private UUID id;
    @Column(name = "ride_id", nullable = false, unique = true) private UUID rideId;
    @Column(name = "payment_id", nullable = false) private UUID paymentId;
    @Column(name = "driver_user_id", nullable = false) private UUID driverUserId;
    @Column(name = "recipient_code", nullable = false, length = 100) private String recipientCode;
    @Column(name = "provider_reference", nullable = false, unique = true, length = 100) private String providerReference;
    @Column(name = "transfer_code", length = 100) private String transferCode;
    @Column(name = "amount_minor", nullable = false) private long amountMinor;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected DriverPayout() {}
    public DriverPayout(UUID rideId, UUID paymentId, UUID driverUserId, String recipientCode, String reference, long amountMinor) {
        this.id = UUID.randomUUID(); this.rideId = rideId; this.paymentId = paymentId; this.driverUserId = driverUserId;
        this.recipientCode = recipientCode; this.providerReference = reference; this.amountMinor = amountMinor;
        this.currency = "NGN"; this.status = "INITIALIZED"; this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
    }
    public void update(String status, String transferCode) {
        this.status = status; if (transferCode != null) this.transferCode = transferCode; this.updatedAt = Instant.now();
    }
    public UUID getId() { return id; }
    public UUID getRideId() { return rideId; }
    public UUID getPaymentId() { return paymentId; }
    public UUID getDriverUserId() { return driverUserId; }
    public String getRecipientCode() { return recipientCode; }
    public String getProviderReference() { return providerReference; }
    public String getTransferCode() { return transferCode; }
    public long getAmountMinor() { return amountMinor; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
