package com.example.acres;

import com.example.acres.entity.RefreshToken;
import com.example.acres.entity.Role;
import com.example.acres.entity.User;
import com.example.acres.exception.UnauthorizedException;
import com.example.acres.repository.RefreshTokenRepository;
import com.example.acres.repository.UserRepository;
import com.example.acres.security.JwtService;
import com.example.acres.service.AuditService;
import com.example.acres.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock RefreshTokenRepository tokens;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;
    @Mock AuditService audit;
    @Mock com.example.acres.security.LoginRateLimiter rateLimiter;
    @InjectMocks AuthService authService;

    private User activeUser;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(authService, "days", 30L);
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setEmail("ravi@example.com");
        activeUser.setName("Ravi");
        activeUser.setRole(Role.USER);
        activeUser.setActive(true);
        activeUser.setPasswordHash("hash");
    }

    @Test
    void loginFailsForInactiveUser() {
        activeUser.setActive(false);
        when(users.findByEmailIgnoreCase("ravi@example.com")).thenReturn(Optional.of(activeUser));
        assertThrows(UnauthorizedException.class, () -> authService.login("ravi@example.com", "pw", "1.1.1.1", "ua"));
    }

    @Test
    void refreshRejectsRevokedTokenAndRevokesAllSessions() {
        RefreshToken old = new RefreshToken();
        old.setUser(activeUser);
        old.setRevokedAt(Instant.now().minusSeconds(120));
        when(tokens.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(old));
        assertThrows(UnauthorizedException.class, () -> authService.refresh("token", "1.1.1.1", "ua"));
        verify(tokens).revokeAllForUser(1L);
    }

    @Test
    void loginSuccessUpdatesLastLogin() {
        when(users.findByEmailIgnoreCase("ravi@example.com")).thenReturn(Optional.of(activeUser));
        when(encoder.matches("pw", "hash")).thenReturn(true);
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwt.create(1L, "ravi@example.com", "USER", 0L)).thenReturn("access");
        when(tokens.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        var session = authService.login("ravi@example.com", "pw", "1.1.1.1", "ua");
        assertEquals("access", session.response().accessToken());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        assert captor.getValue().getLastLoginAt() != null;
        verify(audit).log(any(), anyString(), anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void logoutRevokesToken() {
        RefreshToken token = new RefreshToken();
        token.setUser(activeUser);
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(tokens.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        authService.logout("token", "1.1.1.1");
        verify(tokens).save(token);
        verify(audit).log(any(), anyString(), anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void loginFailsForUnknownEmail() {
        when(users.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        assertThrows(UnauthorizedException.class, () -> authService.login("missing@example.com", "pw", "1.1.1.1", "ua"));
        verify(tokens, never()).save(any());
    }
}
