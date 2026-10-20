package com.chekwube.uber.ride;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class RideDtos {
    private RideDtos() {}

    public record CreateRideRequest(
        @DecimalMin("-90.0") @DecimalMax("90.0") double pickupLatitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") double pickupLongitude,
        @DecimalMin("-90.0") @DecimalMax("90.0") double destinationLatitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") double destinationLongitude
    ) {}

    public record RideResponse(
        UUID id, UUID riderUserId, UUID driverUserId, RideStatus status,
        double pickupLatitude, double pickupLongitude,
        double destinationLatitude, double destinationLongitude,
        BigDecimal estimatedDistanceKm, BigDecimal estimatedFareNaira,
        BigDecimal finalFareNaira, Instant requestedAt, Instant acceptedAt,
        Instant arrivingAt, Instant startedAt, Instant completedAt,
        Instant cancelledAt, UUID cancelledBy, Instant updatedAt
    ) {
        static RideResponse from(Ride r) {
            return new RideResponse(r.getId(), r.getRiderUserId(), r.getDriverUserId(), r.getStatus(),
                r.getPickupLatitude(), r.getPickupLongitude(), r.getDestinationLatitude(), r.getDestinationLongitude(),
                r.getEstimatedDistanceKm(), r.getEstimatedFareNaira(), r.getFinalFareNaira(), r.getRequestedAt(),
                r.getAcceptedAt(), r.getArrivingAt(), r.getStartedAt(), r.getCompletedAt(), r.getCancelledAt(),
                r.getCancelledBy(), r.getUpdatedAt());
        }
    }
}