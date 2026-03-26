package com.financetracker.service;

import com.financetracker.dto.TransactionDtos;
import com.financetracker.entity.Account;
import com.financetracker.entity.Category;
import com.financetracker.entity.FinanceTransaction;
import com.financetracker.entity.TransactionType;
import com.financetracker.entity.User;
import com.financetracker.exception.BadRequestException;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserContextService userContextService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final MapperService mapperService;
    private final AccessControlService accessControlService;
    private final RulesEngineService rulesEngineService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public TransactionDtos.TransactionPageResponse getTransactions(
            int page,
            int size,
            String accountId,
            String categoryId,
            String type,
            String search,
            String startDate,
            String endDate
    ) {
        Set<UUID> accessibleAccountIds = accessControlService.getAccessibleAccountIds();
        Pageable pageable = PageRequest.of(page, size);
        String searchPattern = search == null || search.isBlank() ? null : "%" + search.toLowerCase(Locale.ROOT).trim() + "%";
        Page<FinanceTransaction> result = transactionRepository.searchByAccountIds(
                accessibleAccountIds,
                accountId == null || accountId.isBlank() ? null : UUID.fromString(accountId),
                categoryId == null || categoryId.isBlank() ? null : UUID.fromString(categoryId),
                type == null || type.isBlank() ? null : TransactionType.valueOf(type.toUpperCase()),
                searchPattern,
                startDate == null || startDate.isBlank() ? null : java.time.LocalDate.parse(startDate),
                endDate == null || endDate.isBlank() ? null : java.time.LocalDate.parse(endDate),
                pageable
        );
        return new TransactionDtos.TransactionPageResponse(
                result.getContent().stream().map(mapperService::toTransactionResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public TransactionDtos.TransactionResponse getTransaction(String id) {
        return mapperService.toTransactionResponse(getAccessibleTransaction(id));
    }

    @Transactional
    public TransactionDtos.TransactionResponse create(TransactionDtos.TransactionRequest request) {
        TransactionDtos.TransactionRequest evaluatedRequest = rulesEngineService.applyRules(request);
        validateTransaction(evaluatedRequest);
        User user = userContextService.getCurrentUserEntity();
        Account account = accountService.getWritableAccount(evaluatedRequest.accountId());
        Category category = evaluatedRequest.categoryId() == null || evaluatedRequest.categoryId().isBlank() ? null : categoryService.getOwnedCategory(evaluatedRequest.categoryId());
        FinanceTransaction saved = createForUser(
                user,
                account,
                category,
                evaluatedRequest.type(),
                evaluatedRequest.amount(),
                evaluatedRequest.date(),
                evaluatedRequest.transferAccountId(),
                evaluatedRequest.merchant(),
                evaluatedRequest.note(),
                evaluatedRequest.paymentMethod(),
                evaluatedRequest.tags() == null ? new HashSet<>() : new HashSet<>(evaluatedRequest.tags())
        );
        return mapperService.toTransactionResponse(saved);
    }

    @Transactional
    public TransactionDtos.TransactionResponse update(String id, TransactionDtos.TransactionRequest request) {
        TransactionDtos.TransactionRequest evaluatedRequest = rulesEngineService.applyRules(request);
        validateTransaction(evaluatedRequest);
        FinanceTransaction existing = getAccessibleTransaction(id);
        rollbackAccountImpact(existing);

        Account account = accountService.getWritableAccount(evaluatedRequest.accountId());
        Category category = evaluatedRequest.categoryId() == null || evaluatedRequest.categoryId().isBlank() ? null : categoryService.getOwnedCategory(evaluatedRequest.categoryId());
        existing.setAccount(account);
        existing.setCategory(category);
        existing.setType(evaluatedRequest.type());
        existing.setAmount(evaluatedRequest.amount());
        existing.setTransactionDate(evaluatedRequest.date());
        existing.setMerchant(evaluatedRequest.merchant());
        existing.setNote(evaluatedRequest.note());
        existing.setPaymentMethod(evaluatedRequest.paymentMethod());
        existing.setTransferAccountId(evaluatedRequest.transferAccountId() == null || evaluatedRequest.transferAccountId().isBlank() ? null : UUID.fromString(evaluatedRequest.transferAccountId()));
        existing.setTags(evaluatedRequest.tags() == null ? new HashSet<>() : new HashSet<>(evaluatedRequest.tags()));
        applyAccountImpact(account, evaluatedRequest.type(), evaluatedRequest.amount(), evaluatedRequest.transferAccountId(), true);
        FinanceTransaction saved = transactionRepository.save(existing);
        activityLogService.record(account, "UPDATE", "TRANSACTION", account.getName(), "Updated " + saved.getType() + " transaction for " + saved.getAmount());
        return mapperService.toTransactionResponse(saved);
    }

    @Transactional
    public void delete(String id) {
        FinanceTransaction transaction = getAccessibleTransaction(id);
        rollbackAccountImpact(transaction);
        activityLogService.record(transaction.getAccount(), "DELETE", "TRANSACTION", transaction.getMerchant() == null ? transaction.getType().name() : transaction.getMerchant(), "Deleted transaction for " + transaction.getAmount());
        transactionRepository.delete(transaction);
    }

    @Transactional
    public FinanceTransaction createForUser(
            User user,
            Account account,
            Category category,
            TransactionType type,
            BigDecimal amount,
            LocalDate date,
            String transferAccountId,
            String merchant,
            String note,
            String paymentMethod,
            java.util.Set<String> tags
    ) {
        FinanceTransaction transaction = FinanceTransaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .type(type)
                .amount(amount)
                .transactionDate(date)
                .merchant(merchant)
                .note(note)
                .paymentMethod(paymentMethod)
                .transferAccountId(transferAccountId == null || transferAccountId.isBlank() ? null : UUID.fromString(transferAccountId))
                .tags(tags)
                .build();
        applyAccountImpact(account, type, amount, transferAccountId, true);
        FinanceTransaction saved = transactionRepository.save(transaction);
        activityLogService.record(account, "CREATE", "TRANSACTION", merchant == null || merchant.isBlank() ? type.name() : merchant, "Added " + type + " transaction for " + amount);
        log.info("Created transaction {} for user {}", saved.getId(), user.getEmail());
        return saved;
    }

    @Transactional(readOnly = true)
    public FinanceTransaction getAccessibleTransaction(String id) {
        return transactionRepository.findByIdAndAccountIdIn(UUID.fromString(id), accessControlService.getAccessibleAccountIds())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }

    private void validateTransaction(TransactionDtos.TransactionRequest request) {
        if (request.type() != TransactionType.TRANSFER && (request.categoryId() == null || request.categoryId().isBlank())) {
            throw new BadRequestException("Category is required for income and expense transactions");
        }
        if (request.type() == TransactionType.TRANSFER && (request.transferAccountId() == null || request.transferAccountId().isBlank())) {
            throw new BadRequestException("Transfer destination account is required");
        }
    }

    private void rollbackAccountImpact(FinanceTransaction transaction) {
        applyAccountImpact(transaction.getAccount(), transaction.getType(), transaction.getAmount(),
                transaction.getTransferAccountId() == null ? null : transaction.getTransferAccountId().toString(), false);
    }

    private void applyAccountImpact(Account account, TransactionType type, BigDecimal amount, String transferAccountId, boolean forward) {
        BigDecimal signed = forward ? amount : amount.negate();
        switch (type) {
            case INCOME -> account.setCurrentBalance(account.getCurrentBalance().add(signed));
            case EXPENSE -> account.setCurrentBalance(account.getCurrentBalance().subtract(signed));
            case TRANSFER -> {
                if (transferAccountId == null || transferAccountId.isBlank()) {
                    throw new BadRequestException("Transfer account is required");
                }
                Account transferAccount = accountService.getWritableAccount(transferAccountId);
                if (account.getId().equals(transferAccount.getId())) {
                    throw new BadRequestException("Transfer accounts must be different");
                }
                account.setCurrentBalance(account.getCurrentBalance().subtract(signed));
                transferAccount.setCurrentBalance(transferAccount.getCurrentBalance().add(signed));
            }
        }
    }
}
