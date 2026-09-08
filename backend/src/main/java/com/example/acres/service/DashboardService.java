package com.example.acres.service;

import com.example.acres.dto.DashboardDto;
import com.example.acres.dto.LeaderboardDtos.LeaderboardResponse;
import com.example.acres.dto.LeaderboardDtos.LeaderboardResponse.Entry;
import com.example.acres.dto.SaleDtos.SaleResponse;
import com.example.acres.entity.ProjectSettings;
import com.example.acres.entity.User;
import com.example.acres.repository.ProjectSettingsRepository;
import com.example.acres.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {
    private final ProjectSettingsRepository projects;
    private final SaleRepository sales;
    private final SaleService saleService;

    public DashboardService(ProjectSettingsRepository projects, SaleRepository sales, SaleService saleService) {
        this.projects = projects;
        this.sales = sales;
        this.saleService = saleService;
    }

    public DashboardDto get(User me) {
        ProjectSettings p = projects.getSettings();
        Instant now = Instant.now();
        BigDecimal total = p.getTotalAcres();
        BigDecimal sold = sales.sumAcres().setScale(4, RoundingMode.HALF_UP);
        BigDecimal remaining = total.subtract(sold).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
        BigDecimal progress = total.signum() == 0
                ? BigDecimal.ZERO
                : sold.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);

        ChallengeMetricsCalculator.Metrics metrics = ChallengeMetricsCalculator.calculate(
                total, sold, remaining, p.getStartDate(), p.getDeadline(), p.getTimezone(), now);

        List<DashboardDto.LeaderboardItem> board = buildLeaderboard(sold);
        int myRank = board.stream().filter(x -> x.userId().equals(me.getId())).mapToInt(DashboardDto.LeaderboardItem::rank).findFirst().orElse(0);
        BigDecimal myAcres = board.stream().filter(x -> x.userId().equals(me.getId())).map(DashboardDto.LeaderboardItem::acresSold).findFirst().orElse(BigDecimal.ZERO);
        long myCount = saleService.countForUser(me);
        BigDecimal myPct = total.signum() == 0
                ? BigDecimal.ZERO
                : myAcres.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
        BigDecimal myDailyRate = metrics.daysElapsed() == 0
                ? BigDecimal.ZERO
                : myAcres.divide(BigDecimal.valueOf(metrics.daysElapsed()), 4, RoundingMode.HALF_UP);

        List<SaleResponse> recent = sales.findTop10ByOrderBySaleDateDescCreatedAtDesc().stream().map(saleService::dto).toList();

        return new DashboardDto(
                p.getProjectName(),
                p.getTimezone(),
                total,
                sold,
                remaining,
                progress,
                p.getDeadline(),
                now,
                metrics.secondsUntilDeadline(),
                metrics.daysElapsed(),
                metrics.daysRemaining(),
                metrics.currentDailyRate(),
                metrics.requiredDailyRate(),
                metrics.paceDifference(),
                metrics.projectedCompletionDate(),
                metrics.status(),
                board,
                new DashboardDto.MyPerformance(me.getId(), myRank, myAcres, myPct, myCount, myDailyRate),
                recent);
    }

    public LeaderboardResponse leaderboard(User me) {
        BigDecimal sold = sales.sumAcres().setScale(4, RoundingMode.HALF_UP);
        List<DashboardDto.LeaderboardItem> board = buildLeaderboard(sold);
        List<Entry> entries = board.stream()
                .map(e -> new Entry(e.rank(), e.userId(), e.name(), e.acresSold(), e.percentageOfTeamSales()))
                .toList();
        Entry current = board.stream()
                .filter(e -> e.userId().equals(me.getId()))
                .findFirst()
                .map(e -> new Entry(e.rank(), e.userId(), e.name(), e.acresSold(), e.percentageOfTeamSales()))
                .orElse(new Entry(0, me.getId(), me.getName(), BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP), BigDecimal.ZERO));
        return new LeaderboardResponse(entries, current, sold);
    }

    private List<DashboardDto.LeaderboardItem> buildLeaderboard(BigDecimal sold) {
        List<DashboardDto.LeaderboardItem> board = new ArrayList<>();
        List<Object[]> rows = sales.leaderboard();
        int rank = 1;
        for (Object[] row : rows) {
            Long uid = ((Number) row[0]).longValue();
            String name = (String) row[1];
            BigDecimal acres = (BigDecimal) row[2];
            BigDecimal pct = sold.signum() == 0
                    ? BigDecimal.ZERO
                    : acres.multiply(BigDecimal.valueOf(100)).divide(sold, 1, RoundingMode.HALF_UP);
            board.add(new DashboardDto.LeaderboardItem(rank++, uid, name, acres, pct));
        }
        return board;
    }
}
