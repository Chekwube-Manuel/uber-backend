package com.chekwube.uber.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "driver_profiles")
public class DriverProfile {
    @Id @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "full_name", nullable = false, length = 120) private String fullName;
    @Column(name = "phone_number", nullable = false, unique = true, length = 20) private String phoneNumber;
    @Column(name = "vehicle_make", nullable = false, length = 80) private String vehicleMake;
    @Column(name = "vehicle_model", nullable = false, length = 80) private String vehicleModel;
    @Column(name = "vehicle_color", nullable = false, length = 40) private String vehicleColor;
    @Column(name = "vehicle_plate", nullable = false, unique = true, length = 20) private String vehiclePlate;
    @Column(name = "verification_status", nullable = false, length = 20) private String verificationStatus = "PENDING";
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected DriverProfile() {}
    public DriverProfile(UUID userId, String fullName, String phoneNumber, String vehicleMake, String vehicleModel, String vehicleColor, String vehiclePlate) {
        this.userId = userId; this.fullName = fullName.trim(); this.phoneNumber = phoneNumber.trim(); this.vehicleMake = vehicleMake.trim(); this.vehicleModel = vehicleModel.trim(); this.vehicleColor = vehicleColor.trim(); this.vehiclePlate = normalizePlate(vehiclePlate); this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
    }
    public void update(String fullName, String phoneNumber, String vehicleMake, String vehicleModel, String vehicleColor, String vehiclePlate) {
        this.fullName = fullName.trim(); this.phoneNumber = phoneNumber.trim(); this.vehicleMake = vehicleMake.trim(); this.vehicleModel = vehicleModel.trim(); this.vehicleColor = vehicleColor.trim(); this.vehiclePlate = normalizePlate(vehiclePlate); this.updatedAt = Instant.now();
    }
    private String normalizePlate(String plate) { return plate.trim().toUpperCase(java.util.Locale.ROOT); }
    public UUID getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getVehicleMake() { return vehicleMake; }
    public String getVehicleModel() { return vehicleModel; }
    public String getVehicleColor() { return vehicleColor; }
    public String getVehiclePlate() { return vehiclePlate; }
    public String getVerificationStatus() { return verificationStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
