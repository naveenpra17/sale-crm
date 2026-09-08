package com.example.acres.security;

import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Issues and validates CSRF tokens for cross-origin SPAs where the XSRF-TOKEN cookie
 * may not be sent (third-party cookie restrictions). Double-submit via cookie still
 * works when the browser sends the cookie.
 */
@Service
public class CsrfTokenService {
    private static final long TTL_SECONDS = 3600;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Instant> active = new ConcurrentHashMap<>();

    public String issue() {
        purgeExpired();
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        active.put(token, Instant.now().plusSeconds(TTL_SECONDS));
        return token;
    }

    public boolean isValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Instant expires = active.get(token);
        if (expires == null || expires.isBefore(Instant.now())) {
            active.remove(token);
            return false;
        }
        return true;
    }

    public boolean headerMatchesCookie(String header, String cookie) {
        if (header == null || cookie == null) {
            return false;
        }
        return MessageDigest.isEqual(header.getBytes(), cookie.getBytes());
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        Iterator<Map.Entry<String, Instant>> it = active.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isBefore(now)) {
                it.remove();
            }
        }
    }
}
