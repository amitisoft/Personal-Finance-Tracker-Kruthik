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
        UUID userId = userContextService.getCurrentUserEntity().getId();
        Pageable pageable = PageRequest.of(page, size);
        String searchPattern = search == null || search.isBlank() ? null : "%" + search.toLowerCase(Locale.ROOT).trim() + "%";
        Page<FinanceTransaction> result = transactionRepository.search(
                userId,
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
        return mapperService.toTransactionResponse(getOwnedTransaction(id));
    }

    @Transactional
    public TransactionDtos.TransactionResponse create(TransactionDtos.TransactionRequest request) {
        validateTransaction(request);
        User user = userContextService.getCurrentUserEntity();
        Account account = accountService.getOwnedAccount(request.accountId());
        Category category = request.categoryId() == null || request.categoryId().isBlank() ? null : categoryService.getOwnedCategory(request.categoryId());
        FinanceTransaction saved = createForUser(
                user,
                account,
                category,
                request.type(),
                request.amount(),
                request.date(),
                request.transferAccountId(),
                request.merchant(),
                request.note(),
                request.paymentMethod(),
                request.tags() == null ? new HashSet<>() : new HashSet<>(request.tags())
        );
        return mapperService.toTransactionResponse(saved);
    }

    @Transactional
    public TransactionDtos.TransactionResponse update(String id, TransactionDtos.TransactionRequest request) {
        validateTransaction(request);
        FinanceTransaction existing = getOwnedTransaction(id);
        rollbackAccountImpact(existing);

        Account account = accountService.getOwnedAccount(request.accountId());
        Category category = request.categoryId() == null || request.categoryId().isBlank() ? null : categoryService.getOwnedCategory(request.categoryId());
        existing.setAccount(account);
        existing.setCategory(category);
        existing.setType(request.type());
        existing.setAmount(request.amount());
        existing.setTransactionDate(request.date());
        existing.setMerchant(request.merchant());
        existing.setNote(request.note());
        existing.setPaymentMethod(request.paymentMethod());
        existing.setTransferAccountId(request.transferAccountId() == null || request.transferAccountId().isBlank() ? null : UUID.fromString(request.transferAccountId()));
        existing.setTags(request.tags() == null ? new HashSet<>() : new HashSet<>(request.tags()));
        applyAccountImpact(account, request.type(), request.amount(), request.transferAccountId(), true);
        return mapperService.toTransactionResponse(transactionRepository.save(existing));
    }

    @Transactional
    public void delete(String id) {
        FinanceTransaction transaction = getOwnedTransaction(id);
        rollbackAccountImpact(transaction);
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
        log.info("Created transaction {} for user {}", saved.getId(), user.getEmail());
        return saved;
    }

    @Transactional(readOnly = true)
    public FinanceTransaction getOwnedTransaction(String id) {
        UUID userId = userContextService.getCurrentUserEntity().getId();
        return transactionRepository.findByIdAndUserId(UUID.fromString(id), userId)
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
                Account transferAccount = accountService.getOwnedAccount(transferAccountId);
                if (account.getId().equals(transferAccount.getId())) {
                    throw new BadRequestException("Transfer accounts must be different");
                }
                account.setCurrentBalance(account.getCurrentBalance().subtract(signed));
                transferAccount.setCurrentBalance(transferAccount.getCurrentBalance().add(signed));
            }
        }
    }
}
