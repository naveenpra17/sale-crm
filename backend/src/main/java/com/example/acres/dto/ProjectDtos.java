package com.example.acres.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class ProjectDtos {
    public record ProjectRequest(
            @NotBlank @Size(max = 255) String projectName,
            @NotNull @DecimalMin("0.0001") BigDecimal totalAcres,
            @NotNull LocalDate startDate,
            @NotBlank String deadlineLocal,
            @NotBlank @Size(max = 80) String timezone) {}

    public record ProjectResponse(
            Long id,
            String projectName,
            BigDecimal totalAcres,
            LocalDate startDate,
            Instant deadline,
            String deadlineLocal,
            String timezone,
            Instant updatedAt) {}
}
