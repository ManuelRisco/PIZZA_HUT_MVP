package com.ecopizza.controllers;

import com.ecopizza.dtos.ApiResponse;
import com.ecopizza.dtos.ProfitLossDashboardDTO;
import com.ecopizza.services.FinancialDashboardService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class FinancialDashboardController {

    private final FinancialDashboardService financialDashboardService;

    public FinancialDashboardController(FinancialDashboardService financialDashboardService) {
        this.financialDashboardService = financialDashboardService;
    }

    @GetMapping("/profit-loss")
    public ResponseEntity<ApiResponse<ProfitLossDashboardDTO>> getProfitLoss(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "groupBy", defaultValue = "DAY") String groupBy,
            @RequestParam(value = "onlyDelivered", defaultValue = "false") boolean onlyDelivered) {
        return ResponseEntity.ok(ApiResponse.success(
                financialDashboardService.getProfitLossReport(startDate, endDate, groupBy, onlyDelivered)));
    }
}

