package com.financetracker.dto;

import java.util.List;

public class ActivityDtos {

    public record ActivityResponse(
            String id,
            String accountId,
            String actorName,
            String actorEmail,
            String action,
            String resourceType,
            String resourceName,
            String details,
            String createdAt
    ) {
    }

    public record ActivityFeedResponse(
            String accountId,
            String accountName,
            List<ActivityResponse> activities
    ) {
    }
}
