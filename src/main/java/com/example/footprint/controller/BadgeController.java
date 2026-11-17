package com.example.footprint.controller;

import com.example.footprint.dto.BadgeListResponse;
import com.example.footprint.service.BadgeService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/badges")
public class BadgeController {

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    // 현재 사용자가 획득한 배지 목록 조회
    @GetMapping("/my")
    public BadgeListResponse getMyBadges() {
        return badgeService.getMyBadges();
    }
}
