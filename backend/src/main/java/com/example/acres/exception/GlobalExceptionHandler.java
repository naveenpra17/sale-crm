package com.example.acres.exception;

import com.example.acres.config.RequestIdFilter;
import com.example.acres.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e, HttpServletRequest r) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        String msg = fields.values().stream().findFirst().orElse("Validation failed");
        return ResponseEntity.badRequest().body(error(400, "VALIDATION_ERROR", msg, r, fields));
    }

    @ExceptionHandler({IllegalArgumentException.class, BadRequestException.class})
    ResponseEntity<ErrorResponse> bad(RuntimeException e, HttpServletRequest r) {
        return ResponseEntity.badRequest().body(error(400, "BAD_REQUEST", e.getMessage(), r, null));
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ErrorResponse> unauthorized(UnauthorizedException e, HttpServletRequest r) {
        return ResponseEntity.status(401).body(error(401, "UNAUTHORIZED", e.getMessage(), r, null));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> auth(AuthenticationException e, HttpServletRequest r) {
        return ResponseEntity.status(401).body(error(401, "UNAUTHORIZED", "Authentication required", r, null));
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ErrorResponse> forbidden(ForbiddenException e, HttpServletRequest r) {
        return ResponseEntity.status(403).body(error(403, "FORBIDDEN", e.getMessage(), r, null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException e, HttpServletRequest r) {
        return ResponseEntity.status(403).body(error(403, "FORBIDDEN", "Access denied", r, null));
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ErrorResponse> conflict(ConflictException e, HttpServletRequest r) {
        return ResponseEntity.status(409).body(error(409, "CONFLICT", e.getMessage(), r, null));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    ResponseEntity<ErrorResponse> rateLimited(TooManyRequestsException e, HttpServletRequest r) {
        return ResponseEntity.status(429).body(error(429, "RATE_LIMITED", e.getMessage(), r, null));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ErrorResponse> optimisticLock(ObjectOptimisticLockingFailureException e, HttpServletRequest r) {
        return ResponseEntity.status(409).body(error(409, "CONFLICT", "Project settings were updated by another session. Please refresh and try again.", r, null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException e, HttpServletRequest r) {
        return ResponseEntity.status(409).body(error(409, "CONFLICT", "Data conflict. The record may already exist or is referenced elsewhere.", r, null));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> any(Exception e, HttpServletRequest r) {
        log.error("requestId={} unhandled error on {}", requestId(r), r.getRequestURI(), e);
        return ResponseEntity.status(500).body(error(500, "INTERNAL_ERROR", "An unexpected error occurred", r, null));
    }

    private ErrorResponse error(int status, String code, String message, HttpServletRequest r, Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, code, message, r.getRequestURI(), requestId(r), fieldErrors);
    }

    private String requestId(HttpServletRequest r) {
        String fromMdc = MDC.get(RequestIdFilter.MDC_KEY);
        if (fromMdc != null) {
            return fromMdc;
        }
        return r.getHeader(RequestIdFilter.HEADER);
    }
}
