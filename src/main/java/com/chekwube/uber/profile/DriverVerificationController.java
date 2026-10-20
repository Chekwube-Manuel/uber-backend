package com.chekwube.uber.profile;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/drivers")
public class DriverVerificationController {
    private final DriverVerificationService service;

    public DriverVerificationController(DriverVerificationService service) {
        this.service = service;
    }

    @PutMapping("/{driverId}/verification")
    public ProfileDtos.DriverVerificationResponse verify(
        Authentication authentication,
        @PathVariable UUID driverId,
        @Valid @RequestBody ProfileDtos.DriverVerificationRequest request
    ) {
        return service.verify(driverId, request.decision(), (UUID) authentication.getPrincipal());
    }
}