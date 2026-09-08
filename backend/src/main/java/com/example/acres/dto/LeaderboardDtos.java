package com.example.acres.dto;

import java.math.BigDecimal;
import java.util.List;

public class LeaderboardDtos {
    public record LeaderboardResponse(List<Entry> entries, Entry currentUser, BigDecimal teamTotalAcres) {
        public record Entry(int rank, Long userId, String name, BigDecimal acresSold, BigDecimal percentageOfTeamSales) {}
    }
}
