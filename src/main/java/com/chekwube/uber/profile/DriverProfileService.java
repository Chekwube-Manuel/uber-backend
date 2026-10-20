package com.chekwube.uber.profile;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;
import java.util.UUID;

@Service
public class DriverProfileService {
    private final DriverProfileRepository profiles;
    public DriverProfileService(DriverProfileRepository profiles) { this.profiles = profiles; }
    @Transactional(readOnly = true)
    public ProfileDtos.DriverProfileResponse get(UUID userId) {
        return profiles.findById(userId).map(this::toResponse).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver profile not found"));
    }
    @Transactional
    public ProfileDtos.DriverProfileResponse upsert(UUID userId, ProfileDtos.DriverProfileRequest request) {
        String phone = request.phoneNumber().trim();
        String plate = request.vehiclePlate().trim().toUpperCase(Locale.ROOT);
        if (profiles.existsByPhoneNumberAndUserIdNot(phone, userId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already in use");
        if (profiles.existsByVehiclePlateAndUserIdNot(plate, userId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle plate is already registered");
        DriverProfile profile = profiles.findById(userId).orElse(null);
        if (profile == null) profile = new DriverProfile(userId, request.fullName(), phone, request.vehicleMake(), request.vehicleModel(), request.vehicleColor(), plate);
        else profile.update(request.fullName(), phone, request.vehicleMake(), request.vehicleModel(), request.vehicleColor(), plate);
        return toResponse(profiles.save(profile));
    }
    private ProfileDtos.DriverProfileResponse toResponse(DriverProfile p) { return new ProfileDtos.DriverProfileResponse(p.getUserId(), p.getFullName(), p.getPhoneNumber(), p.getVehicleMake(), p.getVehicleModel(), p.getVehicleColor(), p.getVehiclePlate(), p.getVerificationStatus(), p.getCreatedAt(), p.getUpdatedAt()); }
}
