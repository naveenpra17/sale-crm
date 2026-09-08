package com.example.acres.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@Configuration
public class ProductionConfigValidator {
    private static final String DEFAULT_SECRET = "change-this-development-secret-to-a-long-random-value-at-least-32-bytes";

    @Value("${app.jwt-secret}")
    private String jwtSecret;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    private final Environment environment;

    public ProductionConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        if (!isProduction()) {
            return;
        }
        if (jwtSecret == null || jwtSecret.length() < 32 || DEFAULT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("JWT_SECRET must be set to a strong random value in production");
        }
        if (frontendUrl == null || frontendUrl.isBlank() || frontendUrl.contains("localhost")) {
            throw new IllegalStateException("FRONTEND_URL must be set to the production Vercel origin");
        }
        boolean hasDb = (databaseUrl != null && !databaseUrl.isBlank())
                || (datasourceUrl != null && !datasourceUrl.isBlank() && !datasourceUrl.contains("localhost"));
        if (!hasDb) {
            throw new IllegalStateException("DATABASE_URL or production datasource configuration is required");
        }
    }

    private boolean isProduction() {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return "production".equalsIgnoreCase(System.getenv("RENDER"))
                || "true".equalsIgnoreCase(System.getenv("PRODUCTION"));
    }
}
