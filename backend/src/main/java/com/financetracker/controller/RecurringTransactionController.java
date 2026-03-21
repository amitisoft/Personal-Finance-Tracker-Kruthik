package com.financetracker.controller;

import com.financetracker.dto.AuthDtos;
import com.financetracker.dto.RecurringDtos;
import com.financetracker.service.RecurringTransactionService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recurring")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;

    @GetMapping
    public ResponseEntity<List<RecurringDtos.RecurringResponse>> getRecurringTransactions() {
        return ResponseEntity.ok(recurringTransactionService.getRecurringTransactions());
    }

    @PostMapping
    public ResponseEntity<RecurringDtos.RecurringResponse> create(@Valid @RequestBody RecurringDtos.RecurringRequest request) {
        return ResponseEntity.ok(recurringTransactionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringDtos.RecurringResponse> update(@PathVariable String id, @Valid @RequestBody RecurringDtos.RecurringRequest request) {
        return ResponseEntity.ok(recurringTransactionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AuthDtos.MessageResponse> delete(@PathVariable String id) {
        recurringTransactionService.delete(id);
        return ResponseEntity.ok(new AuthDtos.MessageResponse("Recurring item deleted successfully"));
    }
}
