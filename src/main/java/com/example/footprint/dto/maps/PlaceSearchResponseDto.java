package com.example.footprint.dto.maps;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSearchResponseDto {
    private String placeName;       // 장소명
    private String addressName;     // 지번 주소
    private String roadAddressName; // 도로명 주소
    private Double latitude;        // 위도 
    private Double longitude;       // 경도 
    private String categoryName;    
}