package com.example.acres.config;

import com.example.acres.entity.Role;
import com.example.acres.entity.User;
import com.example.acres.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitialAdminBootstrapTest {
    @Mock
    UserRepository users;
    @Mock
    PasswordEncoder encoder;
    @Mock
    Environment environment;

    InitialAdminBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        bootstrap = new InitialAdminBootstrap(users, encoder, environment);
        ReflectionTestUtils.setField(bootstrap, "adminEmail", "admin@example.com");
        ReflectionTestUtils.setField(bootstrap, "adminPassword", "");
        ReflectionTestUtils.setField(bootstrap, "adminName", "Admin");
    }

    @Test
    void skipsWhenUsersExist() {
        when(users.count()).thenReturn(1L);
        bootstrap.bootstrap();
        verify(users, never()).save(any());
    }

    @Test
    void skipsWhenSeedProfileActive() {
        when(users.count()).thenReturn(0L);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"seed"});
        bootstrap.bootstrap();
        verify(users, never()).save(any());
    }

    @Test
    void createsAdminWhenDatabaseEmpty() {
        when(users.count()).thenReturn(0L);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(encoder.encode(any())).thenAnswer(inv -> "hash:" + inv.getArgument(0));
        bootstrap.bootstrap();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.isMustChangePassword()).isTrue();
        assertThat(saved.getPasswordHash()).isEqualTo("hash:ChangeMe123!");
    }
}
