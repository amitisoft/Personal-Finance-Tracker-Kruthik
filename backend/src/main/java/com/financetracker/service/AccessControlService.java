package com.financetracker.service;

import com.financetracker.entity.Account;
import com.financetracker.entity.AccountMember;
import com.financetracker.entity.AccountMembershipRole;
import com.financetracker.entity.User;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.exception.UnauthorizedException;
import com.financetracker.repository.AccountMemberRepository;
import com.financetracker.repository.AccountRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;
    private final UserContextService userContextService;

    @Transactional(readOnly = true)
    public Set<UUID> getAccessibleAccountIds() {
        User user = userContextService.getCurrentUserEntity();
        Set<UUID> ids = new LinkedHashSet<>();
        accountRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).forEach(account -> ids.add(account.getId()));
        accountMemberRepository.findByUserId(user.getId()).forEach(member -> ids.add(member.getAccount().getId()));
        return ids;
    }

    @Transactional(readOnly = true)
    public Account requireAccountReadAccess(String accountId) {
        return requireAccountAccess(accountId, false);
    }

    @Transactional(readOnly = true)
    public Account requireAccountWriteAccess(String accountId) {
        return requireAccountAccess(accountId, true);
    }

    @Transactional(readOnly = true)
    public AccountMembershipRole getRoleForAccount(UUID accountId) {
        User user = userContextService.getCurrentUserEntity();
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        if (account.getUser().getId().equals(user.getId())) {
            return AccountMembershipRole.OWNER;
        }
        return accountMemberRepository.findByAccountIdAndUserId(accountId, user.getId())
                .map(AccountMember::getRole)
                .orElseThrow(() -> new UnauthorizedException("You do not have access to this account"));
    }

    private Account requireAccountAccess(String accountId, boolean writeRequired) {
        UUID accountUuid = UUID.fromString(accountId);
        Account account = accountRepository.findById(accountUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        AccountMembershipRole role = getRoleForAccount(accountUuid);
        if (writeRequired && role == AccountMembershipRole.VIEWER) {
            throw new UnauthorizedException("You do not have permission to modify this account");
        }
        return account;
    }
}
