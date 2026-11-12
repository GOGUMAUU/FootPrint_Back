package com.example.footprint.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.footprint.dto.GuestbookRequest;
import com.example.footprint.dto.GuestbookResponse;
import com.example.footprint.dto.ReportRequest;
import com.example.footprint.service.GuestbookService;

import lombok.RequiredArgsConstructor;

/**
 * 방명록 작성/상세/수정/삭제/좋아요/열람 기록/신고.
 * 인증이 아직 없어 임시로 X-User-Id 헤더(없으면 1번)를 사용자 식별에 사용합니다. (로그인 구현 후 교체)
 */
@RestController
@RequestMapping("/api/guestbooks")
@RequiredArgsConstructor
public class GuestbookController {

    private static final String USER_HEADER = "X-User-Id";
    private static final String DEFAULT_USER = "1";

    private final GuestbookService guestbookService;

    @PostMapping
    public ResponseEntity<GuestbookResponse> create(
            @RequestHeader(value = USER_HEADER, defaultValue = DEFAULT_USER) Long userId,
            @RequestBody GuestbookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guestbookService.create(userId, request));
    }

    @GetMapping("/{id}")
    public GuestbookResponse get(
            @RequestHeader(value = USER_HEADER, defaultValue = DEFAULT_USER) Long userId,
            @PathVariable Long id,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        return guestbookService.get(userId, id, lat, lng);
    }

    @PutMapping("/{id}")
    public GuestbookResponse update(
            @RequestHeader(value = USER_HEADER, defaultValue = DEFAULT_USER) Long userId,
            @PathVariable Long id,
            @RequestBody GuestbookRequest request) {
        return guestbookService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = USER_HEADER, defaultValue = DEFAULT_USER) Long userId,
            @PathVariable Long id) {
        guestbookService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/likes")
    public ResponseEntity<Void> like(
            @RequestHeader(value = USER_HEADER, defaultValue = DEFAULT_USER) Long userId,
            @PathVariable Long id) {
        guestbookService.like(userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/likes")
    public ResponseEntity<Void> unlike(
            @RequestHeader(value = USER_HEADER, defaultValue = DEFAULT_USER) Long userId,
            @PathVariable Long id) {
        guestbookService.unlike(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/views")
    public ResponseEntity<Void> saveView(
            @RequestHeader(value = USER_HEADER, defaultValue = DEFAULT_USER) Long userId,
            @PathVariable Long id) {
        guestbookService.saveView(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reports")
    public ResponseEntity<Void> report(
            @RequestHeader(value = USER_HEADER, defaultValue = DEFAULT_USER) Long userId,
            @PathVariable Long id,
            @RequestBody ReportRequest request) {
        guestbookService.report(userId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}