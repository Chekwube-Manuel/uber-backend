package com.chekwube.uber.ride;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
public class RideController {
    private final RideService service;

    public RideController(RideService service) { this.service = service; }

    @PostMapping("/api/v1/rider/rides")
    public RideDtos.RideResponse request(Authentication auth, @Valid @RequestBody RideDtos.CreateRideRequest request) {
        return service.request((UUID) auth.getPrincipal(), request);
    }

    @GetMapping("/api/v1/rider/rides")
    public List<RideDtos.RideResponse> riderHistory(Authentication auth) {
        return service.riderHistory((UUID) auth.getPrincipal());
    }

    @GetMapping("/api/v1/rider/rides/{rideId}")
    public RideDtos.RideResponse get(Authentication auth, @PathVariable UUID rideId) {
        return service.getForRider((UUID) auth.getPrincipal(), rideId);
    }

    @PostMapping("/api/v1/rider/rides/{rideId}/cancel")
    public RideDtos.RideResponse cancel(Authentication auth, @PathVariable UUID rideId) {
        return service.cancelByRider((UUID) auth.getPrincipal(), rideId);
    }

    @GetMapping("/api/v1/driver/rides/requests")
    public List<RideDtos.RideResponse> requests() {
        return service.dispatchQueue();
    }

    @GetMapping("/api/v1/driver/rides")
    public List<RideDtos.RideResponse> driverHistory(Authentication auth) {
        return service.driverHistory((UUID) auth.getPrincipal());
    }

    @PostMapping("/api/v1/driver/rides/{rideId}/accept")
    public RideDtos.RideResponse accept(Authentication auth, @PathVariable UUID rideId) {
        return service.accept((UUID) auth.getPrincipal(), rideId);
    }

    @PostMapping("/api/v1/driver/rides/{rideId}/arriving")
    public RideDtos.RideResponse arriving(Authentication auth, @PathVariable UUID rideId) {
        return service.markArriving((UUID) auth.getPrincipal(), rideId);
    }

    @PostMapping("/api/v1/driver/rides/{rideId}/start")
    public RideDtos.RideResponse start(Authentication auth, @PathVariable UUID rideId) {
        return service.start((UUID) auth.getPrincipal(), rideId);
    }

    @PostMapping("/api/v1/driver/rides/{rideId}/complete")
    public RideDtos.RideResponse complete(Authentication auth, @PathVariable UUID rideId) {
        return service.complete((UUID) auth.getPrincipal(), rideId);
    }
}