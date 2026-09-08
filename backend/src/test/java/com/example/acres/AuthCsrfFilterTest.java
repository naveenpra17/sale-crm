package com.example.acres;

import com.example.acres.security.AuthCsrfFilter;
import com.example.acres.security.CsrfTokenService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuthCsrfFilterTest {
    private CsrfTokenService csrfTokenService;
    private AuthCsrfFilter filter;
    private MockHttpServletResponse response;

    @BeforeEach
    void setup() {
        csrfTokenService = new CsrfTokenService();
        filter = new AuthCsrfFilter(csrfTokenService);
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
    void rejectsLoginWithoutCsrfHeaderOrCookie() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
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
    void allowsLoginWhenHeaderMatchesCookie() throws Exception {
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
    void allowsLoginWithHeaderOnly_crossOriginWithoutCookie() throws Exception {
        String token = csrfTokenService.issue();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("X-XSRF-TOKEN", token);
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void allowsRefreshWithHeaderOnly() throws Exception {
        String token = csrfTokenService.issue();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/refresh");
        request.addHeader("X-XSRF-TOKEN", token);
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void allowsLogoutWithHeaderOnly() throws Exception {
        String token = csrfTokenService.issue();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");
        request.addHeader("X-XSRF-TOKEN", token);
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }
}
