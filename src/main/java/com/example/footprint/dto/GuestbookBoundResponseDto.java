package com.example.footprint.dto;

import com.example.footprint.entity.Guestbook;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestbookBoundResponseDto {
    private Long guestbookId;
    private Long userId;
    private String content;
    private Double latitude;
    private Double longitude;
    private String visibility;
    private String locationName;
    private LocalDateTime createdAt;

    // 팀원의 entity/Guestbook.java 기준 변환 메서드
    public static GuestbookBoundResponseDto fromEntity(Guestbook guestbook) {
        return GuestbookBoundResponseDto.builder()
                .guestbookId(guestbook.getGuestbookId())
                .userId(guestbook.getUser() != null ? guestbook.getUser().getUserId() : null)
                .content(guestbook.getContent())
                .latitude(guestbook.getLatitude() != null ? guestbook.getLatitude().doubleValue() : null)
                .longitude(guestbook.getLongitude() != null ? guestbook.getLongitude().doubleValue() : null)
                .visibility(guestbook.getVisibility() != null ? guestbook.getVisibility().name() : null)
                .locationName(guestbook.getLocationName())
                .createdAt(guestbook.getCreatedAt())
                .build();
    }
}