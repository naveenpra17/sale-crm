package com.example.acres.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public final class ChallengeMetricsCalculator {
    private ChallengeMetricsCalculator() {}

    public record Metrics(
            long daysElapsed,
            long daysRemaining,
            double fractionalDaysRemaining,
            long secondsUntilDeadline,
            BigDecimal currentDailyRate,
            BigDecimal requiredDailyRate,
            BigDecimal paceDifference,
            LocalDate projectedCompletionDate,
            String status) {}

    public static Metrics calculate(
            BigDecimal totalAcres,
            BigDecimal soldAcres,
            BigDecimal remainingAcres,
            LocalDate startDate,
            Instant deadline,
            String timezone,
            Instant serverNow) {
        ZoneId zone = ZoneId.of(timezone);
        ZonedDateTime nowZ = serverNow.atZone(zone);
        ZonedDateTime deadlineZ = deadline.atZone(zone);
        long secondsUntilDeadline = ChronoUnit.SECONDS.between(nowZ, deadlineZ);
        LocalDate today = nowZ.toLocalDate();
        long daysElapsed = Math.max(0, ChronoUnit.DAYS.between(startDate, today));
        long daysRemaining = secondsUntilDeadline <= 0
                ? 0
                : (long) Math.ceil(secondsUntilDeadline / 86400.0);
        double fractionalDaysRemaining = secondsUntilDeadline <= 0 ? 0 : secondsUntilDeadline / 86400.0;

        BigDecimal current = daysElapsed == 0
                ? BigDecimal.ZERO
                : soldAcres.divide(BigDecimal.valueOf(daysElapsed), 4, RoundingMode.HALF_UP);

        BigDecimal required = BigDecimal.ZERO;
        if (remainingAcres.signum() > 0 && fractionalDaysRemaining > 0) {
            required = remainingAcres.divide(BigDecimal.valueOf(fractionalDaysRemaining), 4, RoundingMode.HALF_UP);
        }

        BigDecimal paceDifference = current.subtract(required).setScale(4, RoundingMode.HALF_UP);

        LocalDate projected = null;
        if (remainingAcres.signum() == 0) {
            projected = today;
        } else if (current.signum() > 0) {
            projected = today.plusDays(remainingAcres.divide(current, 0, RoundingMode.CEILING).longValue());
        }

        String status = resolveStatus(secondsUntilDeadline, soldAcres, totalAcres, current, required, daysElapsed);

        return new Metrics(daysElapsed, daysRemaining, fractionalDaysRemaining, secondsUntilDeadline,
                current, required, paceDifference, projected, status);
    }

    private static String resolveStatus(
            long secondsUntilDeadline,
            BigDecimal sold,
            BigDecimal total,
            BigDecimal current,
            BigDecimal required,
            long daysElapsed) {
        if (secondsUntilDeadline <= 0) {
            return "DEADLINE_PASSED";
        }
        if (total.signum() > 0 && sold.compareTo(total) >= 0) {
            return "TARGET_REACHED";
        }
        if (sold.signum() == 0 && daysElapsed == 0) {
            return "NOT_STARTED";
        }
        if (required.signum() == 0) {
            return "ON_TRACK";
        }
        int cmp = current.compareTo(required);
        if (cmp >= 0) {
            return cmp == 0 ? "ON_TRACK" : "AHEAD_OF_TARGET";
        }
        return "BEHIND_TARGET";
    }
}
