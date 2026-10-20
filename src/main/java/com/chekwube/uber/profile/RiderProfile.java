package com.chekwube.uber.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rider_profiles")
public class RiderProfile {
    @Id @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "full_name", nullable = false, length = 120) private String fullName;
    @Column(name = "phone_number", nullable = false, unique = true, length = 20) private String phoneNumber;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected RiderProfile() {}
    public RiderProfile(UUID userId, String fullName, String phoneNumber) { this.userId = userId; this.fullName = fullName.trim(); this.phoneNumber = phoneNumber.trim(); this.createdAt = Instant.now(); this.updatedAt = this.createdAt; }
    public void update(String fullName, String phoneNumber) { this.fullName = fullName.trim(); this.phoneNumber = phoneNumber.trim(); this.updatedAt = Instant.now(); }
    public UUID getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
