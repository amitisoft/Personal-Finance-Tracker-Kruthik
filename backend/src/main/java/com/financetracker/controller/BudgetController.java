package com.financetracker.controller;

import com.financetracker.dto.AuthDtos;
import com.financetracker.dto.BudgetDtos;
import com.financetracker.service.BudgetService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<List<BudgetDtos.BudgetResponse>> getBudgets(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        LocalDate now = LocalDate.now();
        return ResponseEntity.ok(budgetService.getBudgets(month == null ? now.getMonthValue() : month, year == null ? now.getYear() : year));
    }

    @PostMapping
    public ResponseEntity<BudgetDtos.BudgetResponse> create(@Valid @RequestBody BudgetDtos.BudgetRequest request) {
        return ResponseEntity.ok(budgetService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetDtos.BudgetResponse> update(@PathVariable String id, @Valid @RequestBody BudgetDtos.BudgetRequest request) {
        return ResponseEntity.ok(budgetService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AuthDtos.MessageResponse> delete(@PathVariable String id) {
        budgetService.delete(id);
        return ResponseEntity.ok(new AuthDtos.MessageResponse("Budget deleted successfully"));
    }
}
