package com.example.footprint.dto;

import java.util.List;
import java.math.BigDecimal;
import com.example.footprint.entity.QuestType;

public record ChallengeListResponse(
        int LocalChallenges_totalCount,
        List<ChallengeItem> challenges
) {
    public record ChallengeItem(
            Long challengeId,
            String title,
            String region,
            String category,
            String iconUrl,
            boolean isCompleted,
            BigDecimal latitude,
            BigDecimal longitude,
            long completedCount,
            Long creatorId,
            QuestType questType
    ) {}
}