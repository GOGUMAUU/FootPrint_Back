package com.example.footprint.dto.maps;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NearbyGuestbookRequestDto {
    private Double latitude;   // 내 위치 위도
    private Double longitude;  // 내 위치 경도
    private Integer zoomLevel; // 지도 줌 레벨
    private String sort;       // (구버전 호환) "popular" 이면 pick=popular 로 취급
    private String pick;       // 묶음 대표 글 기준: "recommended"(기본) | "popular" | "latest"
    private String period;     // 작성 기간: null(전체) | "7d" | "30d"
}