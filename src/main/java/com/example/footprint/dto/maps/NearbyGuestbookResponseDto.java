package com.example.footprint.dto.maps;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyGuestbookResponseDto {

    private Double centerLatitude;
    private Double centerLongitude;
    private Integer totalCount;
    private List<GuestbookItem> guestbooks;

    /** 👣 게시판(클러스터) 발자국 수 */
    public record Footprints(long count, long today) {}

    /** nearby / bound 공통 지도 아이템 */
    @Getter
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuestbookItem {
        private Long guestbookId;
        private Long userId;
        private String title;          // 본문 앞부분 (말풍선용)
        private String content;        // 본문 전체
        private String thumbnailUrl;
        private Double latitude;
        private Double longitude;
        private String locationName;
        private Double distanceMeter;  // bound 조회에선 null
        private Boolean isAccessible;
        private Integer likeCount;
        private Boolean isClustered;   // true면 대표 글 + 주변 글 묶음
        private Integer clusterCount;  // 묶인 글 개수 (대표 포함)
        private List<Long> clusterIds; // 묶인 글 id 목록 (대표 포함, 묶였을 때만). 게시판 목록 조회에 그대로 넘긴다
        private String nickname;       // 게시판 목록 조회에서만 채움
        private String createdAt;      // 게시판 목록 조회에서만 채움 (ISO-8601)
        private Boolean letter;        // 그 자리에서 열리는 편지 (말풍선을 따로 그림, 묶이지 않음)
    }
}