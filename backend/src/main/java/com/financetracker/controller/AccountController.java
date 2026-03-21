package com.financetracker.controller;

import com.financetracker.dto.AccountDtos;
import com.financetracker.dto.AuthDtos;
import com.financetracker.service.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<AccountDtos.AccountResponse>> getAccounts() {
        return ResponseEntity.ok(accountService.getAccounts());
    }

    @PostMapping
    public ResponseEntity<AccountDtos.AccountResponse> create(@Valid @RequestBody AccountDtos.AccountRequest request) {
        return ResponseEntity.ok(accountService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDtos.AccountResponse> update(@PathVariable String id, @Valid @RequestBody AccountDtos.AccountRequest request) {
        return ResponseEntity.ok(accountService.update(id, request));
    }

    @PostMapping("/transfer")
    public ResponseEntity<AuthDtos.MessageResponse> transfer(@Valid @RequestBody AccountDtos.TransferRequest request) {
        accountService.transfer(request);
        return ResponseEntity.ok(new AuthDtos.MessageResponse("Transfer completed successfully"));
    }
}
