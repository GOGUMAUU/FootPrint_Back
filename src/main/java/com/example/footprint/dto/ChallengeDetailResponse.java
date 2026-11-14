package com.example.footprint.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ChallengeDetailResponse(
        Long challengeId,
        String title,
        String description,
        String region,
        BigDecimal targetLatitude,
        BigDecimal targetLongitude,
        Integer radiusMeters,
        RewardBadge rewardBadge,
        LocalDateTime startDate,
        LocalDateTime endDate
) {
    public record RewardBadge(
            Long badgeId,
            String badgeName,
            String badgeIconUrl
    ) {
    }
}
