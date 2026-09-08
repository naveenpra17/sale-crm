package com.example.acres.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UserDtos {
    public record UserRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            @NotNull String role,
            boolean mustChangePassword) {}

    public record UserUpdateRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Email @Size(max = 320) String email,
            @NotNull String role,
            boolean active,
            boolean mustChangePassword) {}
}
