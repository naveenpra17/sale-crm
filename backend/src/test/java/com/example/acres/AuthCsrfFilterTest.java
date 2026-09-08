package com.example.acres;

import com.example.acres.security.AuthCsrfFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuthCsrfFilterTest {
    private AuthCsrfFilter filter;
    private MockHttpServletResponse response;

    @BeforeEach
    void setup() {
        filter = new AuthCsrfFilter();
        response = new MockHttpServletResponse();
    }

    @Test
    void allowsCsrfEndpointWithoutHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/csrf");
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void rejectsLoginWithoutCsrfHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setCookies(new jakarta.servlet.http.Cookie("XSRF-TOKEN", "abc123"));
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        assertEquals(403, response.getStatus());
    }

    @Test
    void rejectsLoginWhenHeaderDoesNotMatchCookie() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setCookies(new jakarta.servlet.http.Cookie("XSRF-TOKEN", "cookie-token"));
        request.addHeader("X-XSRF-TOKEN", "header-token");
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        assertEquals(403, response.getStatus());
    }

    @Test
    void allowsLoginWhenHeaderMatchesCookie_crossOriginDoubleSubmit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        String token = "cross-origin-csrf-token-value";
        request.setCookies(new jakarta.servlet.http.Cookie("XSRF-TOKEN", token));
        request.addHeader("X-XSRF-TOKEN", token);
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void allowsRefreshWhenHeaderMatchesCookie() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/refresh");
        String token = "refresh-csrf-token";
        request.setCookies(new jakarta.servlet.http.Cookie("XSRF-TOKEN", token));
        request.addHeader("X-XSRF-TOKEN", token);
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void allowsLogoutWhenHeaderMatchesCookie() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");
        String token = "logout-csrf-token";
        request.setCookies(new jakarta.servlet.http.Cookie("XSRF-TOKEN", token));
        request.addHeader("X-XSRF-TOKEN", token);
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }
}
