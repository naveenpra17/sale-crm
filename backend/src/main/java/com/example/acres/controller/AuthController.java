package com.example.acres.controller;

import com.example.acres.dto.AuthDtos.AuthResponse;
import com.example.acres.dto.AuthDtos.CsrfResponse;
import com.example.acres.dto.PasswordDtos.ChangePasswordRequest;
import com.example.acres.entity.User;
import com.example.acres.security.AuthCookieService;
import com.example.acres.security.CsrfTokenService;
import com.example.acres.service.AuthService;
import com.example.acres.dto.AuthDtos.LoginRequest;
import com.example.acres.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final UserService users;
    private final AuthCookieService cookies;
    private final CsrfTokenService csrfTokens;

    public AuthController(AuthService auth, UserService users, AuthCookieService cookies, CsrfTokenService csrfTokens) {
        this.auth = auth;
        this.users = users;
        this.cookies = cookies;
        this.csrfTokens = csrfTokens;
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> csrf(HttpServletResponse res) {
        String token = csrfTokens.issue();
        res.addHeader(HttpHeaders.SET_COOKIE, cookies.buildCsrfCookie(token));
        return ResponseEntity.ok(new CsrfResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest r, HttpServletRequest req, HttpServletResponse res) {
        var s = auth.login(r.email(), r.password(), req.getRemoteAddr(), req.getHeader("User-Agent"));
        res.addHeader(HttpHeaders.SET_COOKIE, cookies.buildRefreshCookie(s.refreshToken()));
        return ResponseEntity.ok(s.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest req, HttpServletResponse res) {
        var s = auth.refresh(cookies.get(req), req.getRemoteAddr(), req.getHeader("User-Agent"));
        res.addHeader(HttpHeaders.SET_COOKIE, cookies.buildRefreshCookie(s.refreshToken()));
        return ResponseEntity.ok(s.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req, HttpServletResponse res) {
        auth.logout(cookies.get(req), req.getRemoteAddr());
        res.addHeader(HttpHeaders.SET_COOKIE, cookies.clearRefreshCookie());
        return ResponseEntity.noContent().build();
    }

    /**
     * Mandatory password change after login — uses the HttpOnly refresh cookie so it works
     * cross-origin when the in-memory access token is missing or rejected.
     */
    @PostMapping("/change-password")
    public ResponseEntity<AuthResponse> changePassword(@Valid @RequestBody ChangePasswordRequest r,
                                                         HttpServletRequest req, HttpServletResponse res) {
        User u = auth.userFromRefreshCookie(cookies.get(req));
        users.changePassword(u, r.currentPassword(), r.newPassword(), req.getRemoteAddr());
        var s = auth.createSessionForUser(u, req.getRemoteAddr(), req.getHeader("User-Agent"));
        res.addHeader(HttpHeaders.SET_COOKIE, cookies.buildRefreshCookie(s.refreshToken()));
        return ResponseEntity.ok(s.response());
    }
}
