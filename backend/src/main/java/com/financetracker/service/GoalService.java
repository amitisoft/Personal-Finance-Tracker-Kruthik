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
import java.util.List;
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

    public List<GoalDtos.GoalResponse> getGoals() {
        User user = userContextService.getCurrentUserEntity();
        return goalRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(mapperService::toGoalResponse)
                .toList();
    }

    @Transactional
    public GoalDtos.GoalResponse create(GoalDtos.GoalRequest request) {
        User user = userContextService.getCurrentUserEntity();
        Goal goal = Goal.builder()
                .user(user)
                .name(request.name())
                .targetAmount(request.targetAmount())
                .currentAmount(BigDecimal.ZERO)
                .targetDate(request.targetDate())
                .linkedAccount(request.linkedAccountId() == null || request.linkedAccountId().isBlank() ? null : accountService.getOwnedAccount(request.linkedAccountId()))
                .icon(request.icon())
                .color(request.color())
                .status(GoalStatus.ACTIVE)
                .build();
        return mapperService.toGoalResponse(goalRepository.save(goal));
    }

    @Transactional
    public GoalDtos.GoalResponse update(String id, GoalDtos.GoalRequest request) {
        Goal goal = getOwnedGoal(id);
        goal.setName(request.name());
        goal.setTargetAmount(request.targetAmount());
        goal.setTargetDate(request.targetDate());
        goal.setLinkedAccount(request.linkedAccountId() == null || request.linkedAccountId().isBlank() ? null : accountService.getOwnedAccount(request.linkedAccountId()));
        goal.setIcon(request.icon());
        goal.setColor(request.color());
        syncGoalStatus(goal);
        return mapperService.toGoalResponse(goalRepository.save(goal));
    }

    @Transactional
    public GoalDtos.GoalResponse contribute(String id, GoalDtos.GoalContributionRequest request, boolean withdraw) {
        Goal goal = getOwnedGoal(id);
        BigDecimal delta = withdraw ? request.amount().negate() : request.amount();
        if (withdraw && goal.getCurrentAmount().compareTo(request.amount()) < 0) {
            throw new BadRequestException("Cannot withdraw more than the current goal balance");
        }
        if (request.sourceAccountId() != null && !request.sourceAccountId().isBlank()) {
            Account account = accountService.getOwnedAccount(request.sourceAccountId());
            BigDecimal newBalance = withdraw ? account.getCurrentBalance().add(request.amount()) : account.getCurrentBalance().subtract(request.amount());
            if (!withdraw && newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Insufficient source account balance");
            }
            account.setCurrentBalance(newBalance);
        }
        goal.setCurrentAmount(goal.getCurrentAmount().add(delta));
        syncGoalStatus(goal);
        log.info("Goal {} updated by {}", goal.getId(), delta);
        return mapperService.toGoalResponse(goalRepository.save(goal));
    }

    private void syncGoalStatus(Goal goal) {
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(GoalStatus.COMPLETED);
        } else {
            goal.setStatus(GoalStatus.ACTIVE);
        }
    }

    private Goal getOwnedGoal(String id) {
        UUID userId = userContextService.getCurrentUserEntity().getId();
        return goalRepository.findByIdAndUserId(UUID.fromString(id), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
    }
}
