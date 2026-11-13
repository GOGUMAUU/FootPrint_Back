package com.example.footprint.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.footprint.dto.ApiResponse;
import com.example.footprint.dto.maps.NearbyGuestbookRequestDto;
import com.example.footprint.dto.maps.NearbyGuestbookResponseDto;
import com.example.footprint.dto.maps.PlaceSearchResponseDto;
import com.example.footprint.security.CurrentUserProvider;
import com.example.footprint.service.MapService;
import com.example.footprint.service.PolicyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/maps")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;
    private final CurrentUserProvider current;
    private final PolicyService policies;

    @GetMapping("/guestbooks/bound")
    public ResponseEntity<List<NearbyGuestbookResponseDto.GuestbookItem>> getGuestbooksInBound(
            @RequestParam String scope, // "me", "following", "user"
            @RequestParam(required = false) Long targetUserId,
            @RequestParam Double zoom,
            @RequestParam Double southLat,
            @RequestParam Double westLng,
            @RequestParam Double northLat,
            @RequestParam Double eastLng,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String ageGroup,
            @RequestParam(required = false) String sort // "popular" | "latest"
    ) {
        Long currentUserId = current.id();
        policies.requireLocation(currentUserId);

        List<NearbyGuestbookResponseDto.GuestbookItem> result = mapService.getGuestbooksInBound(
                currentUserId, scope, targetUserId, zoom, southLat, westLng, northLat, eastLng, period, ageGroup, sort
        );
        return ResponseEntity.ok(result);
    }

    /** sort = "distance"(기본) | "popular" */
    @GetMapping("/guestbooks/nearby")
    public ResponseEntity<ApiResponse<NearbyGuestbookResponseDto>> getNearbyGuestbooks(
            @ModelAttribute NearbyGuestbookRequestDto requestDto) {

        Long currentUserId = current.id();
        policies.requireLocation(currentUserId);   // 내 위치를 쓰는 API라 동의 체크 필수

        NearbyGuestbookResponseDto responseData = mapService.getNearbyGuestbooks(requestDto, currentUserId);

        return ResponseEntity.ok(
                ApiResponse.success("주변 방명록 조회가 완료되었습니다.", responseData)
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PlaceSearchResponseDto>>> searchPlaces(
            @RequestParam String keyword) {

        List<PlaceSearchResponseDto> places = mapService.searchPlaces(keyword);

        return ResponseEntity.ok(
                ApiResponse.success("장소 검색이 완료되었습니다.", places)
        );
    }
}