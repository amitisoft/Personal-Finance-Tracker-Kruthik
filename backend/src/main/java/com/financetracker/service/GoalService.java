package com.financetracker.service;

import com.financetracker.dto.GoalDtos;
import com.financetracker.entity.Account;
import com.financetracker.entity.Goal;
import com.financetracker.entity.GoalStatus;
import com.financetracker.entity.User;
import com.financetracker.exception.BadRequestException;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.repository.GoalRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserContextService userContextService;
    private final AccountService accountService;
    private final MapperService mapperService;
    private final AccessControlService accessControlService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<GoalDtos.GoalResponse> getGoals() {
        User user = userContextService.getCurrentUserEntity();
        Set<UUID> accessibleAccountIds = accessControlService.getAccessibleAccountIds();
        Map<UUID, Goal> merged = new LinkedHashMap<>();

        goalRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).forEach(goal -> merged.put(goal.getId(), goal));
        if (!accessibleAccountIds.isEmpty()) {
            goalRepository.findByLinkedAccountIdInOrderByCreatedAtDesc(accessibleAccountIds).forEach(goal -> merged.putIfAbsent(goal.getId(), goal));
        }

        return merged.values().stream()
                .sorted(Comparator.comparing(Goal::getCreatedAt).reversed())
                .map(mapperService::toGoalResponse)
                .toList();
    }

    @Transactional
    public GoalDtos.GoalResponse create(GoalDtos.GoalRequest request) {
        User user = userContextService.getCurrentUserEntity();
        Account linkedAccount = request.linkedAccountId() == null || request.linkedAccountId().isBlank() ? null : accountService.getWritableAccount(request.linkedAccountId());
        Goal goal = Goal.builder()
                .user(user)
                .name(request.name())
                .targetAmount(request.targetAmount())
                .currentAmount(BigDecimal.ZERO)
                .targetDate(request.targetDate())
                .linkedAccount(linkedAccount)
                .icon(request.icon())
                .color(request.color())
                .status(GoalStatus.ACTIVE)
                .build();
        Goal saved = goalRepository.save(goal);
        activityLogService.record(linkedAccount, "CREATE", "GOAL", saved.getName(), "Created shared savings goal");
        return mapperService.toGoalResponse(saved);
    }

    @Transactional
    public GoalDtos.GoalResponse update(String id, GoalDtos.GoalRequest request) {
        Goal goal = getAccessibleGoal(id, true);
        Account linkedAccount = request.linkedAccountId() == null || request.linkedAccountId().isBlank() ? null : accountService.getWritableAccount(request.linkedAccountId());
        goal.setName(request.name());
        goal.setTargetAmount(request.targetAmount());
        goal.setTargetDate(request.targetDate());
        goal.setLinkedAccount(linkedAccount);
        goal.setIcon(request.icon());
        goal.setColor(request.color());
        syncGoalStatus(goal);
        Goal saved = goalRepository.save(goal);
        activityLogService.record(linkedAccount, "UPDATE", "GOAL", saved.getName(), "Updated goal target or linked account");
        return mapperService.toGoalResponse(saved);
    }

    @Transactional
    public GoalDtos.GoalResponse contribute(String id, GoalDtos.GoalContributionRequest request, boolean withdraw) {
        Goal goal = getAccessibleGoal(id, true);
        BigDecimal delta = withdraw ? request.amount().negate() : request.amount();
        if (withdraw && goal.getCurrentAmount().compareTo(request.amount()) < 0) {
            throw new BadRequestException("Cannot withdraw more than the current goal balance");
        }
        if (request.sourceAccountId() != null && !request.sourceAccountId().isBlank()) {
            Account account = accountService.getWritableAccount(request.sourceAccountId());
            BigDecimal newBalance = withdraw ? account.getCurrentBalance().add(request.amount()) : account.getCurrentBalance().subtract(request.amount());
            if (!withdraw && newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Insufficient source account balance");
            }
            account.setCurrentBalance(newBalance);
        }
        goal.setCurrentAmount(goal.getCurrentAmount().add(delta));
        syncGoalStatus(goal);
        Goal saved = goalRepository.save(goal);
        activityLogService.record(goal.getLinkedAccount(), withdraw ? "WITHDRAW" : "CONTRIBUTE", "GOAL", saved.getName(), (withdraw ? "Withdrew " : "Added ") + request.amount());
        log.info("Goal {} updated by {}", goal.getId(), delta);
        return mapperService.toGoalResponse(saved);
    }

    private void syncGoalStatus(Goal goal) {
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(GoalStatus.COMPLETED);
        } else {
            goal.setStatus(GoalStatus.ACTIVE);
        }
    }

    @Transactional(readOnly = true)
    private Goal getAccessibleGoal(String id, boolean writeRequired) {
        Goal goal = goalRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        UUID userId = userContextService.getCurrentUserEntity().getId();
        if (goal.getLinkedAccount() != null) {
            if (writeRequired) {
                accessControlService.requireAccountWriteAccess(goal.getLinkedAccount().getId().toString());
            } else {
                accessControlService.requireAccountReadAccess(goal.getLinkedAccount().getId().toString());
            }
            return goal;
        }
        if (!goal.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Goal not found");
        }
        return goal;
    }
}
