package com.example.acres.security;

import com.example.acres.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository repo;

    public CustomUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        var u = repo.findByEmailIgnoreCase(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return User.withUsername(u.getEmail())
                .password(u.getPasswordHash())
                .roles(u.getRole().name())
                .disabled(!u.isActive())
                .build();
    }
}
