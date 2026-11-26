package com.example.footprint.dto;

import java.time.LocalDateTime;

/** 그 자리에서 열리는 편지: 편지함 목록용 (본문은 그 자리에서 상세로만 볼 수 있어 내려주지 않음) */
public final class LetterDtos {
    private LetterDtos() {}

    public record LetterSummary(
            Long guestbookId,
            Long senderId, String senderNickname,
            Long recipientId, String recipientNickname,
            String locationName, double latitude, double longitude,
            LocalDateTime createdAt,
            boolean opened, LocalDateTime openedAt,
            Double distanceMeter   // 내 위치를 보냈을 때만
    ) {}

    public record UnreadCount(long count) {}
}
