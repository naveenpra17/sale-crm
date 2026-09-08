package com.example.acres.security;

import com.example.acres.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final CustomUserDetailsService uds;
    private final UserRepository users;

    public JwtFilter(JwtService jwt, CustomUserDetailsService uds, UserRepository users) {
        this.jwt = jwt;
        this.uds = uds;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            try {
                Claims c = jwt.parse(h.substring(7));
                var entity = users.findByEmailIgnoreCase(c.getSubject()).orElseThrow();
                if (!entity.isActive()) {
                    throw new JwtException("Inactive user");
                }
                long claimVersion = tokenVersion(c);
                if (claimVersion != entity.getTokenVersion()) {
                    throw new JwtException("Token version mismatch");
                }
                var user = uds.loadUserByUsername(c.getSubject());
                var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | UsernameNotFoundException | IllegalArgumentException ignored) {
                // Invalid token - continue without authentication
            }
        }
        chain.doFilter(req, res);
    }

    private static long tokenVersion(Claims c) {
        Object tv = c.get("tv");
        if (tv instanceof Number n) {
            return n.longValue();
        }
        return -1L;
    }
}
