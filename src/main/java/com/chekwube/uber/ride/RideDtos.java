package com.chekwube.uber.ride;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class RideDtos {
    private RideDtos() {}

    public record CreateRideRequest(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double pickupLatitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double pickupLongitude,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double destinationLatitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double destinationLongitude
    ) {}

    public record RideResponse(
        UUID id, UUID riderUserId, UUID driverUserId, RideStatus status,
        double pickupLatitude, double pickupLongitude,
        double destinationLatitude, double destinationLongitude,
        BigDecimal estimatedDistanceKm, BigDecimal estimatedFareNaira,
        BigDecimal finalFareNaira, BigDecimal cancellationFeeNaira, Instant requestedAt, Instant acceptedAt,
        Instant arrivingAt, Instant startedAt, Instant completedAt,
        Instant cancelledAt, UUID cancelledBy, Instant updatedAt
    ) {
        static RideResponse from(Ride r) {
            return new RideResponse(r.getId(), r.getRiderUserId(), r.getDriverUserId(), r.getStatus(),
                r.getPickupLatitude(), r.getPickupLongitude(), r.getDestinationLatitude(), r.getDestinationLongitude(),
                r.getEstimatedDistanceKm(), r.getEstimatedFareNaira(), r.getFinalFareNaira(), r.getCancellationFeeNaira(), r.getRequestedAt(),
                r.getAcceptedAt(), r.getArrivingAt(), r.getStartedAt(), r.getCompletedAt(), r.getCancelledAt(),
                r.getCancelledBy(), r.getUpdatedAt());
        }
    }
}