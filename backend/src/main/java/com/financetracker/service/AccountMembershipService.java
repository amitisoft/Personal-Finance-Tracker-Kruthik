package com.financetracker.service;

import com.financetracker.dto.AccountMembershipDtos;
import com.financetracker.entity.Account;
import com.financetracker.entity.AccountMember;
import com.financetracker.entity.AccountMembershipRole;
import com.financetracker.entity.User;
import com.financetracker.exception.BadRequestException;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.repository.AccountMemberRepository;
import com.financetracker.repository.AccountRepository;
import com.financetracker.repository.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountMembershipService {

    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;
    private final UserRepository userRepository;
    private final UserContextService userContextService;
    private final AccessControlService accessControlService;
    private final ActivityLogService activityLogService;

    @Transactional
    public AccountMembershipDtos.MemberResponse inviteMember(String accountId, AccountMembershipDtos.InviteRequest request) {
        Account account = requireOwner(accountId);
        User invitedUser = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User with that email was not found"));
        if (account.getUser().getId().equals(invitedUser.getId())) {
            throw new BadRequestException("Account owner already has access");
        }

        AccountMember member = accountMemberRepository.findByAccountIdAndUserId(account.getId(), invitedUser.getId())
                .orElse(AccountMember.builder().account(account).user(invitedUser).build());
        member.setRole(request.role());
        AccountMember saved = accountMemberRepository.save(member);
        activityLogService.record(account, "INVITE", "MEMBER", invitedUser.getDisplayName(), "Granted " + request.role() + " access");
        return toMemberResponse(saved);
    }

    @Transactional(readOnly = true)
    public AccountMembershipDtos.MemberListResponse getMembers(String accountId) {
        Account account = accessControlService.requireAccountReadAccess(accountId);
        List<AccountMembershipDtos.MemberResponse> members = new ArrayList<>();
        members.add(new AccountMembershipDtos.MemberResponse(
                account.getUser().getId().toString(),
                account.getUser().getEmail(),
                account.getUser().getDisplayName(),
                AccountMembershipRole.OWNER
        ));
        accountMemberRepository.findByAccountId(account.getId()).stream()
                .sorted(Comparator.comparing(member -> member.getUser().getDisplayName()))
                .map(this::toMemberResponse)
                .forEach(members::add);
        return new AccountMembershipDtos.MemberListResponse(account.getId().toString(), account.getName(), members);
    }

    @Transactional
    public AccountMembershipDtos.MemberResponse updateMember(String accountId, String userId, AccountMembershipDtos.MemberUpdateRequest request) {
        Account account = requireOwner(accountId);
        AccountMember member = accountMemberRepository.findByAccountIdAndUserId(account.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Account member not found"));
        member.setRole(request.role());
        AccountMember saved = accountMemberRepository.save(member);
        activityLogService.record(account, "UPDATE", "MEMBER", saved.getUser().getDisplayName(), "Changed role to " + request.role());
        return toMemberResponse(saved);
    }

    private Account requireOwner(String accountId) {
        Account account = accountRepository.findById(UUID.fromString(accountId))
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        if (!account.getUser().getId().equals(userContextService.getCurrentUserEntity().getId())) {
            throw new BadRequestException("Only the account owner can manage members");
        }
        return account;
    }

    private AccountMembershipDtos.MemberResponse toMemberResponse(AccountMember member) {
        return new AccountMembershipDtos.MemberResponse(
                member.getUser().getId().toString(),
                member.getUser().getEmail(),
                member.getUser().getDisplayName(),
                member.getRole()
        );
    }
}
