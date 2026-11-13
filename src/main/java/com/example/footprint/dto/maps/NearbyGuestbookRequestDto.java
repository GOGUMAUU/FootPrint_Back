package com.example.footprint.dto.maps;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NearbyGuestbookRequestDto {
    private Double latitude;   // 내 위치 위도
    private Double longitude;  // 내 위치 경도
    private Integer zoomLevel; // 지도 줌 레벨
    private String sort;       // "distance"(기본) | "popular"
}