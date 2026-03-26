package com.financetracker.controller;

import com.financetracker.dto.InsightsDtos;
import com.financetracker.service.InsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightsController {

    private final InsightsService insightsService;

    @GetMapping
    public ResponseEntity<InsightsDtos.InsightsResponse> getInsights() {
        return ResponseEntity.ok(insightsService.getInsights());
    }

    @GetMapping("/health-score")
    public ResponseEntity<InsightsDtos.HealthScoreResponse> getHealthScore() {
        return ResponseEntity.ok(insightsService.getHealthScore());
    }
}
