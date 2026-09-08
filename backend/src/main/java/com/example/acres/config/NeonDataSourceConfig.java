package com.example.acres.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Supports Neon-style {@code postgres://} DATABASE_URL values and configures a
 * conservative Hikari pool for Render → Neon deployments.
 */
@Configuration
public class NeonDataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(
            @Value("${DATABASE_URL:}") String databaseUrl,
            @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/acres}") String springUrl,
            @Value("${DATABASE_USERNAME:postgres}") String username,
            @Value("${DATABASE_PASSWORD:postgres}") String password,
            @Value("${spring.datasource.hikari.maximum-pool-size:5}") int maxPool,
            @Value("${spring.datasource.hikari.minimum-idle:1}") int minIdle,
            @Value("${spring.datasource.hikari.connection-timeout:30000}") long connectionTimeout,
            @Value("${spring.datasource.hikari.idle-timeout:300000}") long idleTimeout,
            @Value("${spring.datasource.hikari.max-lifetime:600000}") long maxLifetime) {

        ParsedUrl parsed = parseUrl(databaseUrl, springUrl, username, password);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(parsed.jdbcUrl);
        config.setUsername(parsed.username);
        config.setPassword(parsed.password);
        config.setMaximumPoolSize(maxPool);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setPoolName("acres-hikari");
        return new HikariDataSource(config);
    }

    private ParsedUrl parseUrl(String databaseUrl, String springUrl, String username, String password) {
        if (databaseUrl != null && !databaseUrl.isBlank()
                && (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
            return fromPostgresUri(databaseUrl);
        }
        return new ParsedUrl(springUrl, username, password);
    }

    private ParsedUrl fromPostgresUri(String uri) {
        try {
            URI u = URI.create(uri.replace("postgres://", "postgresql://"));
            String userInfo = u.getUserInfo();
            String user = userInfo;
            String pass = "";
            if (userInfo != null && userInfo.contains(":")) {
                String[] parts = userInfo.split(":", 2);
                user = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                pass = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
            String query = u.getQuery();
            String jdbc = "jdbc:postgresql://" + u.getHost()
                    + (u.getPort() > 0 ? ":" + u.getPort() : "")
                    + u.getPath()
                    + (query != null && !query.isBlank() ? "?" + query : "?sslmode=require");
            if (!jdbc.contains("sslmode=")) {
                jdbc += (jdbc.contains("?") ? "&" : "?") + "sslmode=require";
            }
            return new ParsedUrl(jdbc, user, pass);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid DATABASE_URL for Neon PostgreSQL", e);
        }
    }

    private record ParsedUrl(String jdbcUrl, String username, String password) {}
}
