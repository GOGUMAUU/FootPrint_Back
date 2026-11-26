package com.example.footprint.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.footprint.dto.LetterDtos.LetterSummary;
import com.example.footprint.dto.LetterDtos.UnreadCount;
import com.example.footprint.entity.Guestbook;
import com.example.footprint.repository.GuestbookRepository;
import com.example.footprint.util.GeoUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LetterService {

    private final GuestbookRepository guestbooks;
    private final UserService access;

    /** 받은 편지함: 차단 관계·탈퇴한 보낸 사람 글은 빠짐 */
    public List<LetterSummary> inbox(Long userId, Double lat, Double lng) {
        return guestbooks.findByRecipient_UserIdAndVisibilityOrderByCreatedAtDesc(userId, Guestbook.Visibility.LETTER)
                .stream()
                .filter(g -> access.canReadGuestbook(userId, g))
                .map(g -> toSummary(g, lat, lng))
                .toList();
    }

    /** 보낸 편지함 */
    public List<LetterSummary> sent(Long userId) {
        return guestbooks.findByUser_UserIdAndVisibilityOrderByCreatedAtDesc(userId, Guestbook.Visibility.LETTER)
                .stream()
                .map(g -> toSummary(g, null, null))
                .toList();
    }

    /** 아직 안 열어 본 받은 편지 수 (편지 도착 표시용) */
    public UnreadCount unread(Long userId) {
        return new UnreadCount(guestbooks.countByRecipient_UserIdAndVisibilityAndLetterOpenedAtIsNull(
                userId, Guestbook.Visibility.LETTER));
    }

    private LetterSummary toSummary(Guestbook g, Double lat, Double lng) {
        Double distance = null;
        if (lat != null && lng != null && Double.isFinite(lat) && Double.isFinite(lng)) {
            distance = Math.round(GeoUtils.distanceMeters(lat, lng,
                    g.getLatitude().doubleValue(), g.getLongitude().doubleValue()) * 10.0) / 10.0;
        }
        var from = g.getUser();
        var to = g.getRecipient();
        return new LetterSummary(
                g.getGuestbookId(),
                from.getUserId(), from.getNickname(),
                to != null ? to.getUserId() : null, to != null ? to.getNickname() : null,
                g.getLocationName(), g.getLatitude().doubleValue(), g.getLongitude().doubleValue(),
                g.getCreatedAt(),
                g.getLetterOpenedAt() != null, g.getLetterOpenedAt(),
                distance);
    }
}
