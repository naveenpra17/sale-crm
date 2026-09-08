package com.example.acres.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SaleDtos {
    public record SaleRequest(
            @NotNull Long userId,
            @NotNull @DecimalMin("0.0001") BigDecimal acres,
            @NotNull LocalDate saleDate,
            @Size(max = 255) String buyerName,
            @Size(max = 255) String plotReference,
            @Size(max = 2000) String notes) {}

    public record SaleResponse(Long id, Long userId, String salesperson, BigDecimal acres, LocalDate saleDate,
                               String buyerName, String plotReference, String notes,
                               java.time.Instant createdAt, java.time.Instant updatedAt) {}
}
