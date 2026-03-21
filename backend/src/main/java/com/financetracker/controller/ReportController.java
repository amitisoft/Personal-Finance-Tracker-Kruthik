package com.financetracker.controller;

import com.financetracker.dto.ReportDtos;
import com.financetracker.service.ReportService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    public ResponseEntity<ReportDtos.DashboardResponse> getDashboard() {
        return ResponseEntity.ok(reportService.getDashboard());
    }

    @GetMapping("/reports/category-spend")
    public ResponseEntity<java.util.List<ReportDtos.CategorySpendPoint>> categorySpend(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return ResponseEntity.ok(reportService.getCategorySpend(startDate, endDate));
    }

    @GetMapping("/reports/income-vs-expense")
    public ResponseEntity<java.util.List<ReportDtos.TrendPoint>> incomeVsExpense(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return ResponseEntity.ok(reportService.getIncomeVsExpense(startDate, endDate));
    }

    @GetMapping("/reports/account-balance-trend")
    public ResponseEntity<java.util.List<ReportDtos.AccountBalancePoint>> accountBalanceTrend() {
        return ResponseEntity.ok(reportService.getAccountBalanceTrend());
    }

    @GetMapping("/reports/summary")
    public ResponseEntity<ReportDtos.FilteredReportResponse> filteredSummary(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return ResponseEntity.ok(reportService.getFilteredReport(startDate, endDate));
    }

    @GetMapping(value = "/reports/export", produces = "text/csv")
    public ResponseEntity<String> exportCsv(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=finance-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(reportService.exportCsv(startDate, endDate));
    }
}
