package com.chekwube.uber.profile;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class RiderProfileService {
    private final RiderProfileRepository profiles;
    public RiderProfileService(RiderProfileRepository profiles) { this.profiles = profiles; }
    @Transactional(readOnly = true)
    public ProfileDtos.RiderProfileResponse get(UUID userId) {
        return profiles.findById(userId).map(this::toResponse).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rider profile not found"));
    }
    @Transactional
    public ProfileDtos.RiderProfileResponse upsert(UUID userId, ProfileDtos.RiderProfileRequest request) {
        String phone = request.phoneNumber().trim();
        if (profiles.existsByPhoneNumberAndUserIdNot(phone, userId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already in use");
        RiderProfile profile = profiles.findById(userId).orElse(null);
        if (profile == null) profile = new RiderProfile(userId, request.fullName(), phone); else profile.update(request.fullName(), phone);
        return toResponse(profiles.save(profile));
    }
    private ProfileDtos.RiderProfileResponse toResponse(RiderProfile p) { return new ProfileDtos.RiderProfileResponse(p.getUserId(), p.getFullName(), p.getPhoneNumber(), p.getCreatedAt(), p.getUpdatedAt()); }
}
