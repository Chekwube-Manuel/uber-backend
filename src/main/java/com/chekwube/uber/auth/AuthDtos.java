package com.chekwube.uber.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}
    public record RegisterRequest(@NotBlank @Email @Size(max = 320) String email, @NotBlank @Size(min = 12, max = 72) String password, UserRole role) {}
    public record LoginRequest(@NotBlank @Email @Size(max = 320) String email, @NotBlank String password) {}
    public record AuthResponse(String accessToken, String tokenType, UUID userId, String email, UserRole role) {}
}
