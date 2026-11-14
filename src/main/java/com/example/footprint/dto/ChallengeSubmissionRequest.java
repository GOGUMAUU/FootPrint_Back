
package com.example.footprint.dto;

public record ChallengeSubmissionRequest(
        String photoUrl,
        String memo,
        double latitude,
        double longitude
) {
}
