package com.chekwube.uber.ride;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
public class RideRequestStreamController {
    private final RideRequestStreamService streams;

    public RideRequestStreamController(RideRequestStreamService streams) {
        this.streams = streams;
    }

    @GetMapping(path = "/api/v1/driver/rides/requests/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication authentication) {
        return streams.subscribe((UUID) authentication.getPrincipal());
    }
}
