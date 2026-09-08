package com.example.acres;

import com.example.acres.security.CsrfTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsrfTokenServiceTest {
    private CsrfTokenService service;

    @BeforeEach
    void setup() {
        service = new CsrfTokenService();
    }

    @Test
    void issuedTokenIsValid() {
        String token = service.issue();
        assertTrue(service.isValid(token));
    }

    @Test
    void unknownTokenIsInvalid() {
        assertFalse(service.isValid("not-a-real-token"));
    }

    @Test
    void headerMatchesCookieWhenEqual() {
        assertTrue(service.headerMatchesCookie("abc", "abc"));
        assertFalse(service.headerMatchesCookie("abc", "xyz"));
        assertFalse(service.headerMatchesCookie(null, "abc"));
    }
}
