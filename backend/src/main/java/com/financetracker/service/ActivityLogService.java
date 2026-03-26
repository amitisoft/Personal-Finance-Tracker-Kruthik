package com.financetracker.service;

import com.financetracker.dto.ActivityDtos;
import com.financetracker.entity.Account;
import com.financetracker.entity.ActivityLog;
import com.financetracker.entity.User;
import com.financetracker.repository.ActivityLogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final AccessControlService accessControlService;
    private final UserContextService userContextService;

    @Transactional
    public void record(Account account, String action, String resourceType, String resourceName, String details) {
        if (account == null) {
            return;
        }
        User actor = userContextService.getCurrentUserEntity();
        activityLogRepository.save(ActivityLog.builder()
                .account(account)
                .actor(actor)
                .action(action)
                .resourceType(resourceType)
                .resourceName(resourceName)
                .details(details)
                .build());
    }

    @Transactional(readOnly = true)
    public ActivityDtos.ActivityFeedResponse getActivity(String accountId) {
        Account account = accessControlService.requireAccountReadAccess(accountId);
        List<ActivityDtos.ActivityResponse> activities = activityLogRepository.findTop20ByAccountIdOrderByCreatedAtDesc(account.getId()).stream()
                .map(activity -> new ActivityDtos.ActivityResponse(
                        activity.getId().toString(),
                        account.getId().toString(),
                        activity.getActor().getDisplayName(),
                        activity.getActor().getEmail(),
                        activity.getAction(),
                        activity.getResourceType(),
                        activity.getResourceName(),
                        activity.getDetails(),
                        activity.getCreatedAt() == null ? null : activity.getCreatedAt().toString()
                ))
                .toList();
        return new ActivityDtos.ActivityFeedResponse(account.getId().toString(), account.getName(), activities);
    }
}
