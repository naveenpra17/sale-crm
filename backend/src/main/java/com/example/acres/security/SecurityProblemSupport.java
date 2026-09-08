package com.example.acres.security;

import com.example.acres.config.RequestIdFilter;
import com.example.acres.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class SecurityProblemSupport implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        write(response, request, 401, "UNAUTHORIZED", "Authentication required");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        write(response, request, 403, "FORBIDDEN", "Access denied");
    }

    private void write(HttpServletResponse response, HttpServletRequest request, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), new ErrorResponse(Instant.now(), status, code, message, request.getRequestURI(), request.getHeader(RequestIdFilter.HEADER), null));
    }
}
