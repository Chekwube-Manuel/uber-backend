package com.chekwube.uber.profile;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DriverPresenceService {
    private final DriverPresenceRepository presenceRepository;
    private final DriverProfileRepository profileRepository;

    public DriverPresenceService(DriverPresenceRepository presenceRepository, DriverProfileRepository profileRepository) {
        this.presenceRepository = presenceRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public ProfileDtos.DriverAvailabilityResponse setAvailability(UUID driverId, boolean available) {
        DriverProfile profile = profileRepository.findById(driverId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver profile not found"));
        if (available && !"APPROVED".equals(profile.getVerificationStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver must be approved before going online");
        }
        DriverPresence presence = presenceRepository.findById(driverId).orElseGet(() -> new DriverPresence(driverId));
        presence.setAvailable(available);
        return toAvailabilityResponse(presenceRepository.save(presence));
    }

    @Transactional
    public ProfileDtos.DriverLocationResponse updateLocation(UUID driverId, double latitude, double longitude) {
        DriverProfile profile = profileRepository.findById(driverId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver profile not found"));
        if (!"APPROVED".equals(profile.getVerificationStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver must be approved before sharing location");
        }
        DriverPresence presence = presenceRepository.findById(driverId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Go online before sharing location"));
        if (!presence.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Go online before sharing location");
        }
        presence.updateLocation(latitude, longitude);
        return toLocationResponse(presenceRepository.save(presence));
    }

    @Transactional(readOnly = true)
    public List<ProfileDtos.NearbyDriverResponse> findNearby(double latitude, double longitude, double radiusKm) {
        double latitudeDelta = radiusKm / 111.0;
        double longitudeScale = Math.max(0.01, Math.abs(Math.cos(Math.toRadians(latitude))));
        double longitudeDelta = Math.min(180.0, radiusKm / (111.0 * longitudeScale));
        List<DriverPresence> matches = presenceRepository.findNearbyAvailableDrivers(
            latitude, longitude, radiusKm,
            Math.max(-90.0, latitude - latitudeDelta), Math.min(90.0, latitude + latitudeDelta),
            Math.max(-180.0, longitude - longitudeDelta), Math.min(180.0, longitude + longitudeDelta)
        );
        return matches.stream().map(p -> new ProfileDtos.NearbyDriverResponse(
            p.getDriverUserId(), p.getLatitude(), p.getLongitude(), p.getLastLocationAt()
        )).toList();
    }

    private ProfileDtos.DriverAvailabilityResponse toAvailabilityResponse(DriverPresence p) {
        return new ProfileDtos.DriverAvailabilityResponse(p.getDriverUserId(), p.isAvailable(), p.getUpdatedAt());
    }

    private ProfileDtos.DriverLocationResponse toLocationResponse(DriverPresence p) {
        return new ProfileDtos.DriverLocationResponse(p.getDriverUserId(), p.getLatitude(), p.getLongitude(), p.getLastLocationAt());
    }
}