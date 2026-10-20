package com.chekwube.uber.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "driver_presence")
public class DriverPresence {
    @Id
    @Column(name = "driver_user_id", nullable = false)
    private UUID driverUserId;

    @Column(name = "is_available", nullable = false)
    private boolean available;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "last_location_at")
    private Instant lastLocationAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DriverPresence() {}

    public DriverPresence(UUID driverUserId) {
        this.driverUserId = driverUserId;
        this.available = false;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void setAvailable(boolean available) {
        this.available = available;
        this.updatedAt = Instant.now();
    }

    public void updateLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.lastLocationAt = Instant.now();
        this.updatedAt = this.lastLocationAt;
    }

    public UUID getDriverUserId() { return driverUserId; }
    public boolean isAvailable() { return available; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Instant getLastLocationAt() { return lastLocationAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}