package com.example.acres.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String requestId,
        Map<String, String> fieldErrors) {

    public ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
        this(timestamp, status, error, message, path, null, null);
    }
}
