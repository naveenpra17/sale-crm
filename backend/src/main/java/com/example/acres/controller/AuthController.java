package com.example.acres.controller;

import com.example.acres.dto.AuthDtos.AuthResponse;
import com.example.acres.dto.AuthDtos.CsrfResponse;
import com.example.acres.security.AuthCookieService;
import com.example.acres.service.AuthService;
import com.example.acres.dto.AuthDtos.LoginRequest;
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

import java.security.SecureRandom;
import java.util.Base64;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final AuthCookieService cookies;

    public AuthController(AuthService auth, AuthCookieService cookies) {
        this.auth = auth;
        this.cookies = cookies;
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> csrf(HttpServletResponse res) {
        byte[] b = new byte[24];
        new SecureRandom().nextBytes(b);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
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
}
