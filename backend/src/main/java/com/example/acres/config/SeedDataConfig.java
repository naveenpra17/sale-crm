package com.example.acres.config;

import com.example.acres.entity.ProjectSettings;
import com.example.acres.entity.Sale;
import com.example.acres.entity.User;
import com.example.acres.repository.ProjectSettingsRepository;
import com.example.acres.repository.SaleRepository;
import com.example.acres.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Configuration
@Profile("seed")
public class SeedDataConfig implements CommandLineRunner {
    private final UserRepository users;
    private final ProjectSettingsRepository projects;
    private final SaleRepository sales;
    private final PasswordEncoder encoder;

    public SeedDataConfig(UserRepository users, ProjectSettingsRepository projects, SaleRepository sales, PasswordEncoder encoder) {
        this.users = users;
        this.projects = projects;
        this.sales = sales;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }
        User admin = user("Admin", "admin@example.com", "ChangeMe123!", com.example.acres.entity.Role.ADMIN);
        User ravi = user("Ravi", "ravi@example.com", "ChangeMe123!", com.example.acres.entity.Role.USER);
        User kumar = user("Kumar", "kumar@example.com", "ChangeMe123!", com.example.acres.entity.Role.USER);
        User arun = user("Arun", "arun@example.com", "ChangeMe123!", com.example.acres.entity.Role.USER);
        users.saveAll(List.of(admin, ravi, kumar, arun));

        if (projects.count() == 0) {
            ProjectSettings p = new ProjectSettings();
            p.setProjectName("50 Acre Sales Challenge");
            p.setTotalAcres(new BigDecimal("50.0000"));
            p.setStartDate(LocalDate.now(ZoneId.of("Asia/Kolkata")));
            p.setDeadline(LocalDateTime.of(2026, 12, 31, 23, 59, 59).atZone(ZoneId.of("Asia/Kolkata")).toInstant());
            p.setTimezone("Asia/Kolkata");
            projects.save(p);
        }

        create(ravi, "8.5000");
        create(kumar, "7.2500");
        create(arun, "6.7500");
    }

    private User user(String name, String email, String password, com.example.acres.entity.Role role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(password));
        u.setRole(role);
        u.setActive(true);
        return u;
    }

    private void create(User user, String acres) {
        Sale s = new Sale();
        s.setUser(user);
        s.setAcres(new BigDecimal(acres));
        s.setSaleDate(LocalDate.now());
        s.setCreatedBy(user);
        s.setUpdatedBy(user);
        sales.save(s);
    }
}
