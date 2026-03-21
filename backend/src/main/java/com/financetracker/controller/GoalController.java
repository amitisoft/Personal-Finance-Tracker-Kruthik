package com.financetracker.controller;

import com.financetracker.dto.GoalDtos;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.financetracker.service.GoalService;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    public ResponseEntity<List<GoalDtos.GoalResponse>> getGoals() {
        return ResponseEntity.ok(goalService.getGoals());
    }

    @PostMapping
    public ResponseEntity<GoalDtos.GoalResponse> create(@Valid @RequestBody GoalDtos.GoalRequest request) {
        return ResponseEntity.ok(goalService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalDtos.GoalResponse> update(@PathVariable String id, @Valid @RequestBody GoalDtos.GoalRequest request) {
        return ResponseEntity.ok(goalService.update(id, request));
    }

    @PostMapping("/{id}/contribute")
    public ResponseEntity<GoalDtos.GoalResponse> contribute(@PathVariable String id, @Valid @RequestBody GoalDtos.GoalContributionRequest request) {
        return ResponseEntity.ok(goalService.contribute(id, request, false));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<GoalDtos.GoalResponse> withdraw(@PathVariable String id, @Valid @RequestBody GoalDtos.GoalContributionRequest request) {
        return ResponseEntity.ok(goalService.contribute(id, request, true));
    }
}
