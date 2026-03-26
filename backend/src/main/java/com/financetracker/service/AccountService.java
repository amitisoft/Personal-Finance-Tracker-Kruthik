package com.financetracker.service;

import com.financetracker.dto.AccountDtos;
import com.financetracker.entity.Account;
import com.financetracker.entity.AccountMembershipRole;
import com.financetracker.entity.FinanceTransaction;
import com.financetracker.entity.TransactionType;
import com.financetracker.entity.User;
import com.financetracker.exception.BadRequestException;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.repository.AccountRepository;
import com.financetracker.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserContextService userContextService;
    private final MapperService mapperService;
    private final AccessControlService accessControlService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<AccountDtos.AccountResponse> getAccounts() {
        return accountRepository.findByIdInOrderByCreatedAtDesc(accessControlService.getAccessibleAccountIds()).stream()
                .sorted(Comparator.comparing(Account::getCreatedAt).reversed())
                .map(this::toAccessibleResponse)
                .toList();
    }

    @Transactional
    public AccountDtos.AccountResponse create(AccountDtos.AccountRequest request) {
        User user = userContextService.getCurrentUserEntity();
        Account account = Account.builder()
                .user(user)
                .name(request.name())
                .type(request.type())
                .openingBalance(request.openingBalance())
                .currentBalance(request.openingBalance())
                .institutionName(request.institutionName())
                .build();
        Account saved = accountRepository.save(account);
        activityLogService.record(saved, "CREATE", "ACCOUNT", saved.getName(), "Created account with opening balance " + saved.getOpeningBalance());
        return toAccessibleResponse(saved);
    }

    @Transactional
    public AccountDtos.AccountResponse update(String id, AccountDtos.AccountRequest request) {
        Account account = getWritableAccount(id);
        account.setName(request.name());
        account.setType(request.type());
        account.setInstitutionName(request.institutionName());
        if (account.getOpeningBalance().compareTo(request.openingBalance()) != 0) {
            BigDecimal diff = request.openingBalance().subtract(account.getOpeningBalance());
            account.setOpeningBalance(request.openingBalance());
            account.setCurrentBalance(account.getCurrentBalance().add(diff));
        }
        Account saved = accountRepository.save(account);
        activityLogService.record(saved, "UPDATE", "ACCOUNT", saved.getName(), "Updated account details");
        return toAccessibleResponse(saved);
    }

    @Transactional
    public void transfer(AccountDtos.TransferRequest request) {
        Account from = getWritableAccount(request.fromAccountId());
        Account to = getWritableAccount(request.toAccountId());
        if (from.getId().equals(to.getId())) {
            throw new BadRequestException("Transfer accounts must be different");
        }
        if (from.getCurrentBalance().compareTo(request.amount()) < 0) {
            throw new BadRequestException("Insufficient balance for transfer");
        }

        from.setCurrentBalance(from.getCurrentBalance().subtract(request.amount()));
        to.setCurrentBalance(to.getCurrentBalance().add(request.amount()));
        accountRepository.save(from);
        accountRepository.save(to);

        User user = userContextService.getCurrentUserEntity();
        FinanceTransaction transaction = FinanceTransaction.builder()
                .user(user)
                .account(from)
                .type(TransactionType.TRANSFER)
                .amount(request.amount())
                .transactionDate(LocalDate.now())
                .merchant("Account Transfer")
                .note(request.note())
                .transferAccountId(to.getId())
                .build();
        transactionRepository.save(transaction);
        activityLogService.record(from, "TRANSFER_OUT", "ACCOUNT", from.getName(), "Transferred " + request.amount() + " to " + to.getName());
        activityLogService.record(to, "TRANSFER_IN", "ACCOUNT", to.getName(), "Received " + request.amount() + " from " + from.getName());
        log.info("Transfer completed from {} to {} for {}", from.getId(), to.getId(), request.amount());
    }

    @Transactional(readOnly = true)
    public Account getReadableAccount(String id) {
        return accessControlService.requireAccountReadAccess(id);
    }

    @Transactional(readOnly = true)
    public Account getWritableAccount(String id) {
        return accessControlService.requireAccountWriteAccess(id);
    }

    public Account getOwnedAccount(String id) {
        UUID userId = userContextService.getCurrentUserEntity().getId();
        return accountRepository.findByIdAndUserId(UUID.fromString(id), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    private AccountDtos.AccountResponse toAccessibleResponse(Account account) {
        AccountDtos.AccountResponse base = mapperService.toAccountResponse(account);
        AccountMembershipRole role = account.getUser().getId().equals(userContextService.getCurrentUserEntity().getId())
                ? AccountMembershipRole.OWNER
                : accessControlService.getRoleForAccount(account.getId());
        return new AccountDtos.AccountResponse(
                base.id(),
                base.name(),
                base.type(),
                base.openingBalance(),
                base.currentBalance(),
                base.institutionName(),
                base.createdAt(),
                role.name()
        );
    }
}
