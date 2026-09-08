package com.example.acres;

import com.example.acres.entity.Role;
import com.example.acres.entity.User;
import com.example.acres.exception.ForbiddenException;
import com.example.acres.repository.UserRepository;
import com.example.acres.service.AuditService;
import com.example.acres.service.AuthService;
import com.example.acres.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository repo;
    @Mock PasswordEncoder encoder;
    @Mock AuthService auth;
    @Mock AuditService audit;
    @InjectMocks UserService userService;

    private User admin;

    @BeforeEach
    void setup() {
        admin = new User();
        admin.setId(1L);
        admin.setName("Admin");
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
    }

    @Test
    void cannotDeactivateLastActiveAdmin() {
        when(repo.findById(1L)).thenReturn(Optional.of(admin));
        when(repo.findAll()).thenReturn(List.of(admin));
        assertThrows(ForbiddenException.class, () -> userService.delete(1L, admin, "1.1.1.1"));
    }
}
