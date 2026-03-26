package com.financetracker.controller;

import com.financetracker.dto.RuleDtos;
import com.financetracker.service.RulesEngineService;
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
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RulesEngineService rulesEngineService;

    @GetMapping
    public ResponseEntity<List<RuleDtos.RuleResponse>> getRules() {
        return ResponseEntity.ok(rulesEngineService.getRules());
    }

    @PostMapping
    public ResponseEntity<RuleDtos.RuleResponse> create(@Valid @RequestBody RuleDtos.RuleRequest request) {
        return ResponseEntity.ok(rulesEngineService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RuleDtos.RuleResponse> update(@PathVariable String id, @Valid @RequestBody RuleDtos.RuleRequest request) {
        return ResponseEntity.ok(rulesEngineService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        rulesEngineService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
