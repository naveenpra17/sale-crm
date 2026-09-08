package com.example.acres.security;

import com.example.acres.exception.ForbiddenException;
import com.example.acres.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class MustChangePasswordFilter extends OncePerRequestFilter {
    private static final Set<String> ALLOWED = Set.of(
            "/api/me/password",
            "/api/auth/logout",
            "/api/auth/me",
            "/api/me"
    );

    private final UserRepository users;

    public MustChangePasswordFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null && !"anonymousUser".equals(auth.getPrincipal())) {
            var user = users.findByEmailIgnoreCase(auth.getName());
            if (user.isPresent() && user.get().isMustChangePassword() && !ALLOWED.contains(request.getRequestURI())) {
                throw new ForbiddenException("Password change required");
            }
        }
        filterChain.doFilter(request, response);
    }
}
