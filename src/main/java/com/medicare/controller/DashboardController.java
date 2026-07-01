package com.medicare.controller;

import com.medicare.dto.DashboardDTO;
import com.medicare.service.impl.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "KPIs and summary metrics")
@SecurityRequirement(name = "BearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get all dashboard KPIs and metrics")
    public ResponseEntity<DashboardDTO> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboardMetrics());
    }

    @GetMapping("/revenue-chart")
    @Operation(summary = "Get daily revenue data for the last 7 days")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getRevenueChartData() {
        return ResponseEntity.ok(dashboardService.getWeeklyRevenueData());
    }
}


