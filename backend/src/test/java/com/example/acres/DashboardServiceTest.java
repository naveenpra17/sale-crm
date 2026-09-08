package com.example.acres;

import com.example.acres.dto.DashboardDto;
import com.example.acres.dto.LeaderboardDtos.LeaderboardResponse;
import com.example.acres.entity.ProjectSettings;
import com.example.acres.entity.Role;
import com.example.acres.entity.User;
import com.example.acres.repository.ProjectSettingsRepository;
import com.example.acres.repository.SaleRepository;
import com.example.acres.service.DashboardService;
import com.example.acres.service.SaleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock ProjectSettingsRepository projects;
    @Mock SaleRepository sales;
    @Mock SaleService saleService;
    @InjectMocks DashboardService dashboardService;

    private User me;
    private ProjectSettings settings;

    @BeforeEach
    void setup() {
        me = new User();
        me.setId(1L);
        me.setName("Ravi");
        me.setRole(Role.USER);
        settings = new ProjectSettings();
        settings.setProjectName("Challenge");
        settings.setTotalAcres(new BigDecimal("50.0000"));
        settings.setStartDate(LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(10));
        settings.setDeadline(LocalDate.of(2026, 12, 31).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant());
        settings.setTimezone("Asia/Kolkata");
    }

    @Test
    void remainingNeverNegativeWhenOversold() {
        when(projects.getSettings()).thenReturn(settings);
        when(sales.sumAcres()).thenReturn(new BigDecimal("52.0000"));
        when(sales.leaderboard()).thenReturn(List.of());
        when(saleService.recentSales(10)).thenReturn(List.of());
        when(saleService.countForUser(me)).thenReturn(0L);
        DashboardDto dto = dashboardService.get(me);
        assertEquals(new BigDecimal("0.0000"), dto.remainingAcres());
        assertEquals(new BigDecimal("104.00"), dto.progressPercent());
    }

    @Test
    void myPerformanceUsesUserDailyRate() {
        when(projects.getSettings()).thenReturn(settings);
        when(sales.sumAcres()).thenReturn(new BigDecimal("20.0000"));
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{1L, "Ravi", new BigDecimal("8.0000")});
        when(sales.leaderboard()).thenReturn(rows);
        when(saleService.recentSales(10)).thenReturn(List.of());
        when(saleService.countForUser(me)).thenReturn(2L);
        DashboardDto dto = dashboardService.get(me);
        assertEquals(new BigDecimal("0.8000"), dto.myPerformance().dailyRate());
    }

    @Test
    void projectionUnavailableWhenNoSales() {
        when(projects.getSettings()).thenReturn(settings);
        when(sales.sumAcres()).thenReturn(BigDecimal.ZERO);
        when(sales.leaderboard()).thenReturn(List.of());
        when(saleService.recentSales(10)).thenReturn(List.of());
        when(saleService.countForUser(me)).thenReturn(0L);
        DashboardDto dto = dashboardService.get(me);
        assertNull(dto.projectedCompletionDate());
        assertEquals("BEHIND_TARGET", dto.status());
    }

    @Test
    void leaderboardHandlesNumericAggregateTypes() {
        when(sales.sumAcres()).thenReturn(new BigDecimal("10.0000"));
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{1L, "Ravi", Double.valueOf(8.5)});
        when(sales.leaderboard()).thenReturn(rows);
        LeaderboardResponse response = dashboardService.leaderboard(me);
        assertEquals(new BigDecimal("8.5"), response.entries().get(0).acresSold());
    }
}
