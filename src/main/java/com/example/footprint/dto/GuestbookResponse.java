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
}
