package com.example.footprint.controller;

import com.example.footprint.dto.GuestbookBoundResponseDto;
import com.example.footprint.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        // 내 사용자 ID (Spring Security Context 등에서 가져온다고 가정)
        Long currentUserId = 1L; 

        List<GuestbookBoundResponseDto> result = mapService.getGuestbooksInBound(
                currentUserId, scope, targetUserId, zoom, southLat, westLng, northLat, eastLng, period, ageGroup, sort
        );
        return ResponseEntity.ok(result);
    }
}