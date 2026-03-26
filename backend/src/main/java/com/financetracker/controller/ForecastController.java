package com.financetracker.controller;

import com.financetracker.dto.ForecastDtos;
import com.financetracker.service.ForecastService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
public class ForecastController {

    private final ForecastService forecastService;

    @GetMapping("/month")
    public ResponseEntity<ForecastDtos.MonthForecastResponse> getMonthForecast() {
        return ResponseEntity.ok(forecastService.getMonthForecast());
    }

    @GetMapping("/daily")
    public ResponseEntity<ForecastDtos.DailyForecastResponse> getDailyForecast() {
        return ResponseEntity.ok(forecastService.getDailyForecast());
    }
}
