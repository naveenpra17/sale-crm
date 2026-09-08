package com.example.acres.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;

@Service
public class AuthCookieService {
    @Value("${app.cookie-secure}")
    boolean secure;
    @Value("${app.cookie-same-site}")
    String sameSite;
    @Value("${app.cookie-domain:}")
    String domain;
    @Value("${app.refresh-token-days}")
    long refreshTokenDays;

    public String refreshName() {
        return "ACRES_REFRESH";
    }

    public String buildCsrfCookie(String token) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from("XSRF-TOKEN", token)
                .httpOnly(false)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ofDays(refreshTokenDays));
        if (domain != null && !domain.isBlank()) {
            b.domain(domain);
        }
        return b.build().toString();
    }

    public String buildRefreshCookie(String token) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(refreshName(), token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/api/auth")
                .maxAge(Duration.ofDays(refreshTokenDays));
        if (domain != null && !domain.isBlank()) {
            b.domain(domain);
        }
        return b.build().toString();
    }

    public String clearRefreshCookie() {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(refreshName(), "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/api/auth")
                .maxAge(Duration.ZERO);
        if (domain != null && !domain.isBlank()) {
            b.domain(domain);
        }
        return b.build().toString();
    }

    public String get(HttpServletRequest req) {
        if (req.getCookies() == null) {
            return null;
        }
        for (Cookie c : req.getCookies()) {
            if (refreshName().equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
