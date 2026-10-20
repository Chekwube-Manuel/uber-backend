package com.chekwube.uber.profile;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/driver/profile")
public class DriverProfileController {
    private final DriverProfileService service;
    public DriverProfileController(DriverProfileService service) { this.service = service; }
    @GetMapping public ProfileDtos.DriverProfileResponse get(Authentication authentication) { return service.get((UUID) authentication.getPrincipal()); }
    @PutMapping public ProfileDtos.DriverProfileResponse upsert(Authentication authentication, @Valid @RequestBody ProfileDtos.DriverProfileRequest request) { return service.upsert((UUID) authentication.getPrincipal(), request); }
}
