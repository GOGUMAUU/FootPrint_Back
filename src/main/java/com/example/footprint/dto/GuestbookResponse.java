package com.example.footprint.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuestbookResponse {
    private Long guestbookId;
    private Long userId;
    private String nickname;
    private String content;
    private double latitude;
    private double longitude;
    private String visibility;
    private String locationName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> photoUrls; // 서버 주소 뒤에 붙이는 경로 (예: /files/xxx.jpg)
    private long likeCount;
    private boolean liked;          // 요청한 사용자가 좋아요 했는지
    private boolean mine;           // 요청한 사용자가 작성자인지
    private boolean letter;         // 그 자리에서 열리는 편지인지
    private Long recipientId;       // 편지 받는 사람
    private String recipientNickname;
    private LocalDateTime letterOpenedAt; // 받는 사람이 처음 열어 본 시각 (아직이면 null)
    private long footprintCount;      // 👣 그 자리에 와서 열어 본 사람 수 (작성자 제외, 편지는 0)
    private long todayFootprintCount; // 그중 오늘(한국 시간) 다녀간 사람 수
}
