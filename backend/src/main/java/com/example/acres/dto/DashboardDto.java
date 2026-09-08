package com.example.acres.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DashboardDto(
        String projectName,
        String timezone,
        BigDecimal totalAcres,
        BigDecimal soldAcres,
        BigDecimal remainingAcres,
        BigDecimal progressPercent,
        Instant deadline,
        Instant serverTime,
        long secondsUntilDeadline,
        long daysElapsed,
        long daysRemaining,
        BigDecimal currentDailyRate,
        BigDecimal requiredDailyRate,
        BigDecimal paceDifference,
        LocalDate projectedCompletionDate,
        String status,
        List<LeaderboardItem> leaderboard,
        MyPerformance myPerformance,
        List<SaleDtos.SaleResponse> recentSales) {

    public record LeaderboardItem(int rank, Long userId, String name, BigDecimal acresSold, BigDecimal percentageOfTeamSales) {}

    public record MyPerformance(Long userId, int rank, BigDecimal acresSold, BigDecimal percentageOfTarget, long salesCount, BigDecimal dailyRate) {}
}
