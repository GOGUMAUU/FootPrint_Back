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

    /** nearby / bound 공통 지도 아이템 */
    @Getter
    @Builder
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
    }
}