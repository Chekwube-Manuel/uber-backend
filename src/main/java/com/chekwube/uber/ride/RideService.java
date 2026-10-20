package com.chekwube.uber.ride;

import com.chekwube.uber.profile.DriverPresence;
import com.chekwube.uber.profile.DriverPresenceRepository;
import com.chekwube.uber.profile.DriverProfile;
import com.chekwube.uber.profile.DriverProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RideService {
    private static final Set<RideStatus> ACTIVE_DRIVER_STATUSES = Set.of(
        RideStatus.ACCEPTED, RideStatus.ARRIVING, RideStatus.IN_PROGRESS
    );

    private final RideRepository rides;
    private final DriverProfileRepository driverProfiles;
    private final DriverPresenceRepository presence;
    private final BigDecimal baseFare;
    private final BigDecimal perKmFare;

    public RideService(RideRepository rides, DriverProfileRepository driverProfiles,
                       DriverPresenceRepository presence,
                       @Value("${app.pricing.base-fare-naira:500}") BigDecimal baseFare,
                       @Value("${app.pricing.per-km-fare-naira:150}") BigDecimal perKmFare) {
        this.rides = rides;
        this.driverProfiles = driverProfiles;
        this.presence = presence;
        this.baseFare = baseFare;
        this.perKmFare = perKmFare;
    }

    @Transactional
    public RideDtos.RideResponse request(UUID riderId, RideDtos.CreateRideRequest request) {
        double distance = distanceKm(request.pickupLatitude(), request.pickupLongitude(),
            request.destinationLatitude(), request.destinationLongitude());
        if (distance < 0.05) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pickup and destination are too close");
        BigDecimal roundedDistance = BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
        BigDecimal fare = baseFare.add(perKmFare.multiply(BigDecimal.valueOf(distance)))
            .setScale(2, RoundingMode.HALF_UP);
        Ride ride = new Ride(riderId, request.pickupLatitude(), request.pickupLongitude(),
            request.destinationLatitude(), request.destinationLongitude(), roundedDistance, fare);
        return RideDtos.RideResponse.from(rides.save(ride));
    }

    @Transactional(readOnly = true)
    public RideDtos.RideResponse getForRider(UUID riderId, UUID rideId) {
        Ride ride = rides.findByIdAndRiderUserId(rideId, riderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        return RideDtos.RideResponse.from(ride);
    }

    @Transactional(readOnly = true)
    public List<RideDtos.RideResponse> riderHistory(UUID riderId) {
        return rides.findTop20ByRiderUserIdOrderByRequestedAtDesc(riderId).stream().map(RideDtos.RideResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RideDtos.RideResponse> driverHistory(UUID driverId) {
        return rides.findTop20ByDriverUserIdOrderByRequestedAtDesc(driverId).stream().map(RideDtos.RideResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RideDtos.RideResponse> dispatchQueue() {
        return rides.findTop50ByStatusAndDriverUserIdIsNullOrderByRequestedAtAsc(RideStatus.REQUESTED)
            .stream().map(RideDtos.RideResponse::from).toList();
    }

    @Transactional
    public RideDtos.RideResponse accept(UUID driverId, UUID rideId) {
        DriverProfile profile = driverProfiles.findById(driverId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver profile not found"));
        if (!"APPROVED".equals(profile.getVerificationStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver must be approved before accepting rides");
        }
        DriverPresence driverPresence = presence.findById(driverId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Go online before accepting rides"));
        if (!driverPresence.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Go online before accepting rides");
        }
        if (rides.existsByDriverUserIdAndStatusIn(driverId, ACTIVE_DRIVER_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Driver already has an active ride");
        }

        Ride ride = rides.findByIdForUpdate(rideId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (ride.getStatus() != RideStatus.REQUESTED || ride.getDriverUserId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ride has already been claimed");
        }
        ride.accept(driverId);
        return RideDtos.RideResponse.from(rides.save(ride));
    }

    @Transactional
    public RideDtos.RideResponse markArriving(UUID driverId, UUID rideId) {
        Ride ride = ownedRideForUpdate(rideId);
        transition(() -> ride.markArriving(driverId));
        return RideDtos.RideResponse.from(rides.save(ride));
    }

    @Transactional
    public RideDtos.RideResponse start(UUID driverId, UUID rideId) {
        Ride ride = ownedRideForUpdate(rideId);
        transition(() -> ride.start(driverId));
        return RideDtos.RideResponse.from(rides.save(ride));
    }

    @Transactional
    public RideDtos.RideResponse complete(UUID driverId, UUID rideId) {
        Ride ride = ownedRideForUpdate(rideId);
        transition(() -> ride.complete(driverId));
        return RideDtos.RideResponse.from(rides.save(ride));
    }

    @Transactional
    public RideDtos.RideResponse cancelByRider(UUID riderId, UUID rideId) {
        Ride ride = rides.findByIdForUpdate(rideId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (!ride.getRiderUserId().equals(riderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found");
        }
        transition(() -> ride.cancel(riderId));
        return RideDtos.RideResponse.from(rides.save(ride));
    }

    private Ride ownedRideForUpdate(UUID rideId) {
        return rides.findByIdForUpdate(rideId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
    }

    private void transition(Runnable action) {
        try { action.run(); }
        catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0088;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0, 1 - a)));
    }
}