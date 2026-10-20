package com.chekwube.uber.ride;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rides")
public class Ride {
    @Id
    private UUID id;

    @Column(name = "rider_user_id", nullable = false)
    private UUID riderUserId;

    @Column(name = "driver_user_id")
    private UUID driverUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RideStatus status;

    @Column(name = "pickup_latitude", nullable = false)
    private double pickupLatitude;
    @Column(name = "pickup_longitude", nullable = false)
    private double pickupLongitude;
    @Column(name = "destination_latitude", nullable = false)
    private double destinationLatitude;
    @Column(name = "destination_longitude", nullable = false)
    private double destinationLongitude;

    @Column(name = "estimated_distance_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedDistanceKm;
    @Column(name = "estimated_fare_naira", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedFareNaira;
    @Column(name = "final_fare_naira", precision = 12, scale = 2)
    private BigDecimal finalFareNaira;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;
    @Column(name = "accepted_at")
    private Instant acceptedAt;
    @Column(name = "arriving_at")
    private Instant arrivingAt;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "cancelled_at")
    private Instant cancelledAt;
    @Column(name = "cancelled_by")
    private UUID cancelledBy;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Ride() {}

    public Ride(UUID riderUserId, double pickupLatitude, double pickupLongitude,
                double destinationLatitude, double destinationLongitude,
                BigDecimal estimatedDistanceKm, BigDecimal estimatedFareNaira) {
        this.id = UUID.randomUUID();
        this.riderUserId = riderUserId;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.estimatedDistanceKm = estimatedDistanceKm;
        this.estimatedFareNaira = estimatedFareNaira;
        this.status = RideStatus.REQUESTED;
        this.requestedAt = Instant.now();
        this.updatedAt = this.requestedAt;
    }

    public void accept(UUID driverId) {
        requireStatus(RideStatus.REQUESTED);
        this.driverUserId = driverId;
        this.status = RideStatus.ACCEPTED;
        this.acceptedAt = Instant.now();
        this.updatedAt = this.acceptedAt;
    }

    public void markArriving(UUID driverId) {
        requireAssignedDriver(driverId);
        requireStatus(RideStatus.ACCEPTED);
        this.status = RideStatus.ARRIVING;
        this.arrivingAt = Instant.now();
        this.updatedAt = this.arrivingAt;
    }

    public void start(UUID driverId) {
        requireAssignedDriver(driverId);
        requireStatus(RideStatus.ARRIVING);
        this.status = RideStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
        this.updatedAt = this.startedAt;
    }

    public void complete(UUID driverId) {
        requireAssignedDriver(driverId);
        requireStatus(RideStatus.IN_PROGRESS);
        this.status = RideStatus.COMPLETED;
        this.finalFareNaira = this.estimatedFareNaira;
        this.completedAt = Instant.now();
        this.updatedAt = this.completedAt;
    }

    public void cancel(UUID actorId) {
        if (status != RideStatus.REQUESTED && status != RideStatus.ACCEPTED && status != RideStatus.ARRIVING) {
            throw new IllegalStateException("Ride cannot be cancelled in its current state");
        }
        this.status = RideStatus.CANCELLED;
        this.cancelledBy = actorId;
        this.cancelledAt = Instant.now();
        this.updatedAt = this.cancelledAt;
    }

    private void requireAssignedDriver(UUID driverId) {
        if (driverUserId == null || !driverUserId.equals(driverId)) {
            throw new IllegalStateException("Ride is assigned to another driver");
        }
    }

    private void requireStatus(RideStatus expected) {
        if (status != expected) throw new IllegalStateException("Invalid ride state transition");
    }

    public UUID getId() { return id; }
    public UUID getRiderUserId() { return riderUserId; }
    public UUID getDriverUserId() { return driverUserId; }
    public RideStatus getStatus() { return status; }
    public double getPickupLatitude() { return pickupLatitude; }
    public double getPickupLongitude() { return pickupLongitude; }
    public double getDestinationLatitude() { return destinationLatitude; }
    public double getDestinationLongitude() { return destinationLongitude; }
    public BigDecimal getEstimatedDistanceKm() { return estimatedDistanceKm; }
    public BigDecimal getEstimatedFareNaira() { return estimatedFareNaira; }
    public BigDecimal getFinalFareNaira() { return finalFareNaira; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getArrivingAt() { return arrivingAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public UUID getCancelledBy() { return cancelledBy; }
    public Instant getUpdatedAt() { return updatedAt; }
}