package com.example.acres.config;

import com.example.acres.entity.Role;
import com.example.acres.entity.User;
import com.example.acres.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first admin when the database has no users (production Neon deploy).
 * The {@code seed} profile remains for local demo data; this covers empty prod DBs.
 */
@Component
public class InitialAdminBootstrap {
    private static final Logger log = LoggerFactory.getLogger(InitialAdminBootstrap.class);
    private static final String DEFAULT_EMAIL = "admin@example.com";
    private static final String DEFAULT_PASSWORD = "ChangeMe123!";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final Environment environment;

    @Value("${app.bootstrap.admin-email:" + DEFAULT_EMAIL + "}")
    private String adminEmail;

    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;

    @Value("${app.bootstrap.admin-name:Admin}")
    private String adminName;

    public InitialAdminBootstrap(UserRepository users, PasswordEncoder encoder, Environment environment) {
        this.users = users;
        this.encoder = encoder;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap() {
        if (users.count() > 0) {
            return;
        }
        if (isSeedProfile()) {
            return;
        }
        String password = resolvePassword();
        User admin = new User();
        admin.setName(adminName);
        admin.setEmail(adminEmail.trim().toLowerCase());
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        admin.setMustChangePassword(DEFAULT_PASSWORD.equals(password));
        users.save(admin);
        log.warn("Created initial admin user {} — change the password after first login", admin.getEmail());
    }

    private String resolvePassword() {
        if (adminPassword != null && !adminPassword.isBlank()) {
            return adminPassword;
        }
        if (isProduction()) {
            log.warn("BOOTSTRAP_ADMIN_PASSWORD not set; using default demo password for empty database");
        }
        return DEFAULT_PASSWORD;
    }

    private boolean isSeedProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("seed".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private boolean isProduction() {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
}
