package com.example.acres.controller;

import com.example.acres.dto.DashboardDto;
import com.example.acres.dto.LeaderboardDtos.LeaderboardResponse;
import com.example.acres.service.CurrentUserService;
import com.example.acres.service.DashboardService;
import com.example.acres.service.SaleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private static final int MAX_PAGE_SIZE = 100;

    private final DashboardService dashboard;
    private final CurrentUserService current;
    private final SaleService sales;

    public DashboardController(DashboardService dashboard, CurrentUserService current, SaleService sales) {
        this.dashboard = dashboard;
        this.current = current;
        this.sales = sales;
    }

    @GetMapping("/dashboard")
    public DashboardDto dashboard() {
        return dashboard.get(current.get());
    }

    @GetMapping("/leaderboard")
    public LeaderboardResponse leaderboard() {
        return dashboard.leaderboard(current.get());
    }

    @GetMapping("/me/sales")
    public Page<?> mySales(@PageableDefault(size = 20) Pageable pageable) {
        return sales.mySalesPage(current.get(), capped(pageable));
    }

    private Pageable capped(Pageable pageable) {
        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        return PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
    }
}
