package com.youai.crm.dashboard;

import java.time.LocalDate;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardResponse summary(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String store,
            Authentication authentication) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = from == null ? today.withDayOfMonth(1) : from;
        LocalDate endDate = to == null ? today : to;
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }
        return dashboardService.summary(startDate, endDate, store, authentication);
    }
}
