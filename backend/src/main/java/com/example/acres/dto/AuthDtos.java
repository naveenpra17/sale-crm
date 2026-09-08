package com.example.acres.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 8, max = 128) String password) {}

    public record AuthResponse(String accessToken, UserResponse user) {}

    public record CsrfResponse(String token) {}

    public record UserResponse(Long id, String name, String email, String role, boolean active,
                               boolean mustChangePassword, java.time.Instant lastLoginAt) {}
}
