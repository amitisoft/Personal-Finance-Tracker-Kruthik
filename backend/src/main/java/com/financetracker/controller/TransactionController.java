package com.financetracker.controller;

import com.financetracker.dto.AuthDtos;
import com.financetracker.dto.TransactionDtos;
import com.financetracker.service.TransactionService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<TransactionDtos.TransactionPageResponse> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return ResponseEntity.ok(transactionService.getTransactions(page, size, accountId, categoryId, type, search, startDate, endDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDtos.TransactionResponse> getTransaction(@PathVariable String id) {
        return ResponseEntity.ok(transactionService.getTransaction(id));
    }

    @PostMapping
    public ResponseEntity<TransactionDtos.TransactionResponse> create(@Valid @RequestBody TransactionDtos.TransactionRequest request) {
        return ResponseEntity.ok(transactionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionDtos.TransactionResponse> update(@PathVariable String id, @Valid @RequestBody TransactionDtos.TransactionRequest request) {
        return ResponseEntity.ok(transactionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AuthDtos.MessageResponse> delete(@PathVariable String id) {
        transactionService.delete(id);
        return ResponseEntity.ok(new AuthDtos.MessageResponse("Transaction deleted successfully"));
    }
}
