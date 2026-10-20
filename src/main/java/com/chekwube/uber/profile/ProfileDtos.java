package com.chekwube.uber.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ProfileDtos {
    private ProfileDtos() {}

    public record RiderProfileRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Pattern(regexp = "^[+0-9][0-9+() .-]{6,19}$", message = "must be a valid phone number") String phoneNumber
    ) {}

    public record RiderProfileResponse(UUID userId, String fullName, String phoneNumber, Instant createdAt, Instant updatedAt) {}

    public record DriverProfileRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Pattern(regexp = "^[+0-9][0-9+() .-]{6,19}$", message = "must be a valid phone number") String phoneNumber,
        @NotBlank @Size(max = 80) String vehicleMake,
        @NotBlank @Size(max = 80) String vehicleModel,
        @NotBlank @Size(max = 40) String vehicleColor,
        @NotBlank @Size(max = 20) @Pattern(regexp = "^[A-Za-z0-9 -]+$", message = "must contain only letters, numbers, spaces, or hyphens") String vehiclePlate
    ) {}

    public record DriverProfileResponse(UUID userId, String fullName, String phoneNumber, String vehicleMake, String vehicleModel, String vehicleColor, String vehiclePlate, String verificationStatus, Instant createdAt, Instant updatedAt) {}
}
