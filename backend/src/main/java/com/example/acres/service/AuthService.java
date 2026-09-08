package com.example.acres.service;

import com.example.acres.dto.AuthDtos.AuthResponse;
import com.example.acres.dto.AuthDtos.UserResponse;
import com.example.acres.entity.RefreshToken;
import com.example.acres.entity.User;
import com.example.acres.exception.UnauthorizedException;
import com.example.acres.repository.RefreshTokenRepository;
import com.example.acres.repository.UserRepository;
import com.example.acres.security.JwtService;
import com.example.acres.security.LoginRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class AuthService {
    public record Session(AuthResponse response, String refreshToken) {}

    private static final long ROTATION_GRACE_SECONDS = 30;

    private final UserRepository users;
    private final RefreshTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;
    private final LoginRateLimiter rateLimiter;

    @Value("${app.refresh-token-days}")
    long days;

    public AuthService(UserRepository users, RefreshTokenRepository tokens, PasswordEncoder encoder, JwtService jwt,
                       AuditService audit, LoginRateLimiter rateLimiter) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
        this.rateLimiter = rateLimiter;
    }

    public Session login(String email, String password, String ip, String ua) {
        String key = email.trim().toLowerCase() + "|" + ip;
        rateLimiter.check(key);
        User u = users.findByEmailIgnoreCase(email.trim()).orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!u.isActive() || !encoder.matches(password, u.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        rateLimiter.reset(key);
        u.setLastLoginAt(Instant.now());
        users.save(u);
        return createSession(u, ip, ua, true);
    }

    @Transactional
    public Session refresh(String raw, String ip, String ua) {
        if (raw == null) {
            throw new UnauthorizedException("Session expired");
        }
        RefreshToken old = tokens.findByTokenHashForUpdate(hash(raw)).orElseThrow(() -> new UnauthorizedException("Session expired"));
        if (old.getRevokedAt() != null) {
            if (isRecentRotation(old)) {
                throw new UnauthorizedException("Session rotating");
            }
            tokens.revokeAllForUser(old.getUser().getId());
            throw new UnauthorizedException("Session expired");
        }
        if (old.getExpiresAt().isBefore(Instant.now()) || !old.getUser().isActive()) {
            throw new UnauthorizedException("Session expired");
        }
        old.setRevokedAt(Instant.now());
        Session next = createSession(old.getUser(), ip, ua, false);
        RefreshToken newToken = tokens.findByTokenHash(hash(next.refreshToken())).orElseThrow();
        old.setReplacedBy(newToken);
        tokens.save(old);
        return next;
    }

    public User userFromRefreshCookie(String raw) {
        if (raw == null) {
            throw new UnauthorizedException("Session expired");
        }
        RefreshToken rt = tokens.findByTokenHash(hash(raw)).orElseThrow(() -> new UnauthorizedException("Session expired"));
        if (rt.getRevokedAt() != null || rt.getExpiresAt().isBefore(Instant.now()) || !rt.getUser().isActive()) {
            throw new UnauthorizedException("Session expired");
        }
        return rt.getUser();
    }

    public Session createSessionForUser(User u, String ip, String ua) {
        return createSession(u, ip, ua, false);
    }

    @Transactional
    public void logout(String raw, String ip) {
        if (raw != null) {
            tokens.findByTokenHash(hash(raw)).ifPresent(t -> {
                if (t.getRevokedAt() == null) {
                    t.setRevokedAt(Instant.now());
                    tokens.save(t);
                    audit.log(t.getUser(), "LOGOUT", "USER", t.getUser().getId().toString(), null, null, ip);
                }
            });
        }
    }

    @Transactional
    public void revokeUserSessions(Long userId) {
        tokens.revokeAllForUser(userId);
    }

    private boolean isRecentRotation(RefreshToken old) {
        return old.getRevokedAt().isAfter(Instant.now().minus(ROTATION_GRACE_SECONDS, ChronoUnit.SECONDS))
                && old.getReplacedBy() != null;
    }

    private Session createSession(User u, String ip, String ua, boolean log) {
        String raw = random();
        RefreshToken rt = new RefreshToken();
        rt.setUser(u);
        rt.setTokenHash(hash(raw));
        rt.setCreatedAt(Instant.now());
        rt.setExpiresAt(Instant.now().plus(days, ChronoUnit.DAYS));
        rt.setIpAddress(ip);
        rt.setUserAgent(ua);
        tokens.save(rt);
        if (log) {
            audit.log(u, "LOGIN", "USER", u.getId().toString(), null, null, ip);
        }
        return new Session(
                new AuthResponse(jwt.create(u.getId(), u.getEmail(), u.getRole().name(), u.getTokenVersion()),
                        new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole().name(), u.isActive(), u.isMustChangePassword(), u.getLastLoginAt())),
                raw);
    }

    private String random() {
        byte[] b = new byte[48];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private String hash(String v) {
        try {
            byte[] b = MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(b);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
