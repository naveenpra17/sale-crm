package com.example.acres.util;

import com.example.acres.entity.Role;
import com.example.acres.exception.BadRequestException;

public final class RoleParser {
    private RoleParser() {}

    public static Role parse(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Role is required");
        }
        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid role: " + value);
        }
    }
}
