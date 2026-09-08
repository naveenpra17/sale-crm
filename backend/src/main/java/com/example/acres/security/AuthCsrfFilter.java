package com.example.acres.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class AuthCsrfFilter extends OncePerRequestFilter {
    private static final String COOKIE = "XSRF-TOKEN";
    private static final String HEADER = "X-XSRF-TOKEN";

    private final CsrfTokenService csrfTokens;

    public AuthCsrfFilter(CsrfTokenService csrfTokens) {
        this.csrfTokens = csrfTokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        boolean protectedPath = path.startsWith("/api/auth/")
                && Set.of("POST").contains(req.getMethod())
                && !path.equals("/api/auth/csrf");

        if (protectedPath) {
            String cookie = readCookie(req);
            String header = req.getHeader(HEADER);
            boolean valid = csrfTokens.headerMatchesCookie(header, cookie)
                    || csrfTokens.isValid(header);
            if (!valid) {
                res.setStatus(403);
                res.setContentType("application/json");
                res.getWriter().write("{\"status\":403,\"error\":\"CSRF\",\"message\":\"CSRF validation failed\"}");
                return;
            }
        }
        chain.doFilter(req, res);
    }

    private String readCookie(HttpServletRequest req) {
        if (req.getCookies() == null) {
            return null;
        }
        for (Cookie c : req.getCookies()) {
            if (COOKIE.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
