package com.example.footprint.dto;

import java.time.LocalDateTime;

public record ChallengeSubmissionResponse(
        Long submissionId,
        Long challengeId,
        String photoUrl,
        String memo,
        String status,
        LocalDateTime submittedAt
) {
}
