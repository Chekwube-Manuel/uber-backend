package com.chekwube.uber.profile;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.UUID;

@Service
public class DriverVerificationService {
    private final DriverProfileRepository profiles;
    private final DriverPresenceRepository presence;

    public DriverVerificationService(DriverProfileRepository profiles, DriverPresenceRepository presence) {
        this.profiles = profiles;
        this.presence = presence;
    }

    @Transactional
    public ProfileDtos.DriverVerificationResponse verify(UUID driverId, DriverVerificationDecision decision, UUID adminId) {
        DriverProfile profile = profiles.findById(driverId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver profile not found"));
        profile.setVerificationStatus(decision.name());
        if (decision == DriverVerificationDecision.REJECTED) {
            presence.findById(driverId).ifPresent(p -> {
                p.setAvailable(false);
                presence.save(p);
            });
        }
        profiles.save(profile);
        return new ProfileDtos.DriverVerificationResponse(driverId, decision, adminId, Instant.now());
    }
}