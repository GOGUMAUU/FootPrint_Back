package com.example.footprint.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BadgeListResponse(
        int totalCount,
        List<BadgeItem> badges
) {
    public record BadgeItem(
            Long questId,
            String badgeName,
            String badgeImageUrl,
            LocalDateTime earnedAt
    ) {
    }
}
