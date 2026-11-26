package com.example.footprint.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.footprint.dto.LetterDtos.LetterSummary;
import com.example.footprint.dto.LetterDtos.UnreadCount;
import com.example.footprint.security.CurrentUserProvider;
import com.example.footprint.service.LetterService;

import lombok.RequiredArgsConstructor;

/**
 * 그 자리에서 열리는 편지함.
 * 편지 쓰기·열기는 기존 방명록 API(visibility=LETTER, recipientId)를 그대로 사용합니다.
 */
@RestController
@RequestMapping("/api/letters")
@RequiredArgsConstructor
public class LetterController {

    private final LetterService letters;
    private final CurrentUserProvider current;

    @GetMapping("/inbox")
    public List<LetterSummary> inbox(@RequestParam(required = false) Double lat,
                                     @RequestParam(required = false) Double lng) {
        return letters.inbox(current.id(), lat, lng);
    }

    @GetMapping("/sent")
    public List<LetterSummary> sent() {
        return letters.sent(current.id());
    }

    @GetMapping("/unread-count")
    public UnreadCount unreadCount() {
        return letters.unread(current.id());
    }
}
