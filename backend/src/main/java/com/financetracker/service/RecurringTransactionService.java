package com.financetracker.service;

import com.financetracker.dto.RecurringDtos;
import com.financetracker.entity.RecurringFrequency;
import com.financetracker.entity.RecurringTransaction;
import com.financetracker.entity.User;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.repository.RecurringTransactionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final UserContextService userContextService;
    private final CategoryService categoryService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final MapperService mapperService;

    @Transactional(readOnly = true)
    public List<RecurringDtos.RecurringResponse> getRecurringTransactions() {
        User user = userContextService.getCurrentUserEntity();
        return recurringTransactionRepository.findByUserIdOrderByNextRunDateAsc(user.getId()).stream()
                .map(mapperService::toRecurringResponse)
                .toList();
    }

    @Transactional
    public RecurringDtos.RecurringResponse create(RecurringDtos.RecurringRequest request) {
        User user = userContextService.getCurrentUserEntity();
        RecurringTransaction recurring = RecurringTransaction.builder()
                .user(user)
                .title(request.title())
                .type(request.type())
                .amount(request.amount())
                .category(request.categoryId() == null || request.categoryId().isBlank() ? null : categoryService.getOwnedCategory(request.categoryId()))
                .account(request.accountId() == null || request.accountId().isBlank() ? null : accountService.getOwnedAccount(request.accountId()))
                .frequency(request.frequency())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .nextRunDate(request.startDate())
                .autoCreateTransaction(request.autoCreateTransaction())
                .paused(Boolean.TRUE.equals(request.paused()))
                .build();
        return mapperService.toRecurringResponse(recurringTransactionRepository.save(recurring));
    }

    @Transactional
    public RecurringDtos.RecurringResponse update(String id, RecurringDtos.RecurringRequest request) {
        RecurringTransaction recurring = getOwnedRecurring(id);
        recurring.setTitle(request.title());
        recurring.setType(request.type());
        recurring.setAmount(request.amount());
        recurring.setCategory(request.categoryId() == null || request.categoryId().isBlank() ? null : categoryService.getOwnedCategory(request.categoryId()));
        recurring.setAccount(request.accountId() == null || request.accountId().isBlank() ? null : accountService.getOwnedAccount(request.accountId()));
        recurring.setFrequency(request.frequency());
        recurring.setStartDate(request.startDate());
        recurring.setEndDate(request.endDate());
        recurring.setAutoCreateTransaction(request.autoCreateTransaction());
        recurring.setPaused(Boolean.TRUE.equals(request.paused()));
        if (recurring.getNextRunDate().isBefore(request.startDate())) {
            recurring.setNextRunDate(request.startDate());
        }
        return mapperService.toRecurringResponse(recurringTransactionRepository.save(recurring));
    }

    @Transactional
    public void delete(String id) {
        recurringTransactionRepository.delete(getOwnedRecurring(id));
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void processRecurringTransactions() {
        LocalDate today = LocalDate.now();
        List<RecurringTransaction> dueItems = recurringTransactionRepository
                .findByPausedFalseAndAutoCreateTransactionTrueAndNextRunDateLessThanEqual(today);
        for (RecurringTransaction recurring : dueItems) {
            if (recurring.getEndDate() != null && recurring.getEndDate().isBefore(today)) {
                recurring.setPaused(true);
                continue;
            }
            if (recurring.getAccount() != null) {
                transactionService.createForUser(
                        recurring.getUser(),
                        recurring.getAccount(),
                        recurring.getCategory(),
                        recurring.getType(),
                        recurring.getAmount(),
                        today,
                        null,
                        recurring.getTitle(),
                        "Auto-generated recurring transaction",
                        "AUTO",
                        Set.of("recurring")
                );
            }
            recurring.setNextRunDate(nextDate(recurring.getNextRunDate(), recurring.getFrequency()));
            recurringTransactionRepository.save(recurring);
            log.info("Processed recurring transaction {}", recurring.getId());
        }
    }

    public LocalDate nextDate(LocalDate date, RecurringFrequency frequency) {
        return switch (frequency) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case YEARLY -> date.plusYears(1);
        };
    }

    @Transactional(readOnly = true)
    public List<RecurringDtos.RecurringResponse> getUpcomingRecurringForUser(UUID userId, int limit) {
        return recurringTransactionRepository.findByUserIdOrderByNextRunDateAsc(userId).stream()
                .filter(item -> !Boolean.TRUE.equals(item.getPaused()))
                .limit(limit)
                .map(mapperService::toRecurringResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    private RecurringTransaction getOwnedRecurring(String id) {
        UUID userId = userContextService.getCurrentUserEntity().getId();
        return recurringTransactionRepository.findByIdAndUserId(UUID.fromString(id), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction not found"));
    }
}
