package com.example.footprint.dto.maps;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NearbyGuestbookRequestDto {
    private Double latitude;   // 현재/중심 위도
    private Double longitude;  // 현재/중심 경도
    private Integer zoomLevel; // 지도 줌 레벨 
}