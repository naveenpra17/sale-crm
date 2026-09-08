package com.example.acres.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

public final class ProjectTimeUtil {
    private ProjectTimeUtil() {}

    public static Instant parseDeadlineLocal(String deadlineLocal, String timezone) {
        try {
            ZoneId zone = ZoneId.of(timezone);
            LocalDateTime local = LocalDateTime.parse(deadlineLocal.trim());
            return local.atZone(zone).toInstant();
        } catch (DateTimeParseException | java.time.zone.ZoneRulesException ex) {
            throw new com.example.acres.exception.BadRequestException("Invalid deadline or timezone");
        }
    }

    public static String formatDeadlineLocal(Instant deadline, String timezone) {
        ZoneId zone = ZoneId.of(timezone);
        return deadline.atZone(zone).toLocalDateTime().toString();
    }
}
