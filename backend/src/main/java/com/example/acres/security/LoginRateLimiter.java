package com.example.acres.security;

import com.example.acres.exception.TooManyRequestsException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory login rate limiter. Suitable for a single Render instance.
 * For multi-instance deployments, use an edge or shared-store limiter.
 */
@Component
public class LoginRateLimiter {
    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_SECONDS = 300;

    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public void check(String key) {
        String normalized = key == null ? "unknown" : key.trim().toLowerCase();
        Instant now = Instant.now();
        AttemptWindow window = attempts.compute(normalized, (k, existing) -> {
            if (existing == null || existing.expiresAt.isBefore(now)) {
                return new AttemptWindow(1, now.plusSeconds(WINDOW_SECONDS));
            }
            existing.count++;
            return existing;
        });
        if (window.count > MAX_ATTEMPTS) {
            throw new TooManyRequestsException("Too many login attempts. Please try again later.");
        }
    }

    public void reset(String key) {
        if (key != null) {
            attempts.remove(key.trim().toLowerCase());
        }
    }

    private static class AttemptWindow {
        int count;
        Instant expiresAt;

        AttemptWindow(int count, Instant expiresAt) {
            this.count = count;
            this.expiresAt = expiresAt;
        }
    }
}
