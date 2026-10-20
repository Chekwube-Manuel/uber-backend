package com.chekwube.uber.profile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@Validated
public class DriverPresenceController {
    private final DriverPresenceService service;

    public DriverPresenceController(DriverPresenceService service) {
        this.service = service;
    }

    @PutMapping("/api/v1/driver/availability")
    public ProfileDtos.DriverAvailabilityResponse setAvailability(
        Authentication authentication,
        @Valid @RequestBody ProfileDtos.DriverAvailabilityRequest request
    ) {
        return service.setAvailability((UUID) authentication.getPrincipal(), request.available());
    }

    @PutMapping("/api/v1/driver/location")
    public ProfileDtos.DriverLocationResponse updateLocation(
        Authentication authentication,
        @Valid @RequestBody ProfileDtos.DriverLocationRequest request
    ) {
        return service.updateLocation((UUID) authentication.getPrincipal(), request.latitude(), request.longitude());
    }

    @GetMapping("/api/v1/rider/nearby-drivers")
    public java.util.List<ProfileDtos.NearbyDriverResponse> nearbyDrivers(
        @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
        @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
        @RequestParam(defaultValue = "5.0") @DecimalMin("0.1") @DecimalMax("25.0") double radiusKm
    ) {
        return service.findNearby(latitude, longitude, radiusKm);
    }
}