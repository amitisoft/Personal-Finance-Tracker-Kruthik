package com.financetracker.controller;

import com.financetracker.dto.AccountMembershipDtos;
import com.financetracker.dto.ActivityDtos;
import com.financetracker.service.AccountMembershipService;
import com.financetracker.service.ActivityLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/{accountId}")
@RequiredArgsConstructor
public class AccountMembershipController {

    private final AccountMembershipService accountMembershipService;
    private final ActivityLogService activityLogService;

    @PostMapping("/invite")
    public ResponseEntity<AccountMembershipDtos.MemberResponse> invite(
            @PathVariable String accountId,
            @Valid @RequestBody AccountMembershipDtos.InviteRequest request
    ) {
        return ResponseEntity.ok(accountMembershipService.inviteMember(accountId, request));
    }

    @GetMapping("/members")
    public ResponseEntity<AccountMembershipDtos.MemberListResponse> members(@PathVariable String accountId) {
        return ResponseEntity.ok(accountMembershipService.getMembers(accountId));
    }

    @PutMapping("/members/{userId}")
    public ResponseEntity<AccountMembershipDtos.MemberResponse> updateMember(
            @PathVariable String accountId,
            @PathVariable String userId,
            @Valid @RequestBody AccountMembershipDtos.MemberUpdateRequest request
    ) {
        return ResponseEntity.ok(accountMembershipService.updateMember(accountId, userId, request));
    }

    @GetMapping("/activity")
    public ResponseEntity<ActivityDtos.ActivityFeedResponse> activity(@PathVariable String accountId) {
        return ResponseEntity.ok(activityLogService.getActivity(accountId));
    }
}
