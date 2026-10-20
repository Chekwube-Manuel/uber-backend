package com.chekwube.uber.profile;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rider/profile")
public class RiderProfileController {
    private final RiderProfileService service;
    public RiderProfileController(RiderProfileService service) { this.service = service; }
    @GetMapping public ProfileDtos.RiderProfileResponse get(Authentication authentication) { return service.get((UUID) authentication.getPrincipal()); }
    @PutMapping public ProfileDtos.RiderProfileResponse upsert(Authentication authentication, @Valid @RequestBody ProfileDtos.RiderProfileRequest request) { return service.upsert((UUID) authentication.getPrincipal(), request); }
}
