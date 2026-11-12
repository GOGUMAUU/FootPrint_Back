package com.example.footprint.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.footprint.dto.ApiResponse;
import com.example.footprint.dto.GuestbookBoundResponseDto;
import com.example.footprint.dto.maps.NearbyGuestbookRequestDto;
import com.example.footprint.dto.maps.NearbyGuestbookResponseDto;
import com.example.footprint.dto.maps.PlaceSearchResponseDto;
import com.example.footprint.service.MapService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/maps")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    @GetMapping("/guestbooks/bound")
    public ResponseEntity<List<GuestbookBoundResponseDto>> getGuestbooksInBound(
            @RequestParam String scope, // "me", "following", "user"
            @RequestParam(required = false) Long targetUserId,
            @RequestParam Double zoom,
            @RequestParam Double southLat,
            @RequestParam Double westLng,
            @RequestParam Double northLat,
            @RequestParam Double eastLng,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String ageGroup,
            @RequestParam(required = false) String sort
    ) {
        Long currentUserId = 1L;

        List<GuestbookBoundResponseDto> result = mapService.getGuestbooksInBound(
                currentUserId, scope, targetUserId, zoom, southLat, westLng, northLat, eastLng, period, ageGroup, sort
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/guestbooks/nearby")
    public ResponseEntity<ApiResponse<NearbyGuestbookResponseDto>> getNearbyGuestbooks(
            @ModelAttribute NearbyGuestbookRequestDto requestDto) {

        Long currentUserId = 1L;

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

}}