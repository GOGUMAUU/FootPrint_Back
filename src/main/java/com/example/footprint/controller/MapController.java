/* package com.example.footprint.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.footprint.service.MapService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/maps")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

 @GetMapping("/guestbooks/nearby")
    public ResponseEntity<ApiResponse<NearbyGuestbookResponseDto>> getNearbyGuestbooks(
            @ModelAttribute NearbyGuestbookRequestDto requestDto) {

        Long currentUserId = 1L;  

        NearbyGuestbookResponseDto responseData = mapService.getNearbyGuestbooks(requestDto, currentUserId);

        return ResponseEntity.ok(
                ApiResponse.success("주변 방명록 조회가 완료되었습니다.", responseData)
        );
} }*/
