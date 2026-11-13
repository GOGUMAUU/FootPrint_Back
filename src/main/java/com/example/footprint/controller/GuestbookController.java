package com.example.footprint.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.data.domain.Page;

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
 * JWT로 인증된 사용자만 방명록 API를 호출할 수 있습니다.
 */
@RestController
@RequestMapping("/api/guestbooks")
@RequiredArgsConstructor
public class GuestbookController {

    private final com.example.footprint.security.CurrentUserProvider current;

    private final GuestbookService guestbookService;

    @PostMapping
    public ResponseEntity<GuestbookResponse> create(
            @RequestBody GuestbookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guestbookService.create(current.id(), request));
    }

    @GetMapping("/{id}")
    public GuestbookResponse get(
            @PathVariable Long id,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        return guestbookService.get(current.id(), id, lat, lng);
    }

    @PutMapping("/{id}")
    public GuestbookResponse update(
            @PathVariable Long id,
            @RequestBody GuestbookRequest request) {
        return guestbookService.update(current.id(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {
        guestbookService.delete(current.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/likes")
    public ResponseEntity<Void> like(
            @PathVariable Long id) {
        guestbookService.like(current.id(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/likes")
    public ResponseEntity<Void> unlike(
            @PathVariable Long id) {
        guestbookService.unlike(current.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/views")
    public ResponseEntity<Void> saveView(
            @PathVariable Long id) {
        guestbookService.saveView(current.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reports")
    public ResponseEntity<Void> report(
            @PathVariable Long id,
            @RequestBody ReportRequest request) {
        guestbookService.report(current.id(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 1. 내 전체 기록 조회 (복합 필터 · 정렬 · 페이지네이션)
    @GetMapping("/me")
    public Page<GuestbookResponse> getMyGuestbooks(
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return guestbookService.getMyGuestbooks(current.id(), period, sort, page, size);
    }

    // 2. 팔로잉 사용자별 기록 조회 (팔로우 승인 검증 + 필터 · 정렬 · 페이지네이션)
    @GetMapping("/users/{userId}")
    public Page<GuestbookResponse> getUserGuestbooks(
            @PathVariable Long userId,
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return guestbookService.getUserGuestbooks(current.id(), userId, period, sort, page, size);
    }
}
