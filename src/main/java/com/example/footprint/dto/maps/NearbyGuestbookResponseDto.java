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

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuestbookItem {
        private Long guestbookId;
        private String title;
        private String thumbnailUrl;
        private Double latitude;
        private Double longitude;
        private String locationName;
        private Double distanceMeter;
        private Boolean isAccessible;
        private Integer likeCount;
        private Boolean isClustered;
        private Integer clusterCount;
    }
}