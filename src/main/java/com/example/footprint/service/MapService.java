package com.example.footprint.service;

import com.example.footprint.entity.Guestbook;
import com.example.footprint.dto.GuestbookBoundResponseDto;
import com.example.footprint.repository.FollowRepository;
import com.example.footprint.repository.GuestbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapService {

    private final GuestbookRepository guestbookRepository;
    private final FollowRepository followRepository;

    public List<GuestbookBoundResponseDto> getGuestbooksInBound(
            Long currentUserId, String scope, Long targetUserId, Double zoom,
            Double southLat, Double westLng, Double northLat, Double eastLng,
            String period, String ageGroup, String sort
    ) {
        List<Long> targetUserIds;

        if ("me".equals(scope)) {
            targetUserIds = Collections.singletonList(currentUserId);
        } else if ("following".equals(scope)) {
            // ACCEPTED 상태의 팔로잉 대상 유저 ID 목록 반환[cite: 3, 4]
            targetUserIds = followRepository.findAcceptedFollowingIds(currentUserId);
        } else if ("user".equals(scope) && targetUserId != null) {
            // 특정 유저 조회 시 팔로우 승인 여부 검증[cite: 3, 4]
            boolean isAccepted = followRepository.existsByFollower_UserIdAndFollowing_UserIdAndStatus(currentUserId, targetUserId, "ACCEPTED");
            if (!isAccepted && !currentUserId.equals(targetUserId)) {
                throw new IllegalArgumentException("팔로우 승인된 사용자만 기록을 조회할 수 있습니다.");
            }
            targetUserIds = Collections.singletonList(targetUserId);
        } else {
            return Collections.emptyList();
        }

        // DB Query (Repository 호출)
        List<Guestbook> guestbooks = guestbookRepository.findInBound(
            targetUserIds,
            BigDecimal.valueOf(southLat),
            BigDecimal.valueOf(northLat),
            BigDecimal.valueOf(westLng),
            BigDecimal.valueOf(eastLng)
        );
        // 일부 대표 기록(예: 상위 N개 또는 특정 조건)만 필터링해서 전달
        if (zoom < 10.0 && guestbooks.size() > 20) {
            guestbooks = guestbooks.subList(0, 20); // 상위 20개만 대표 기록으로 사용
        }

        return guestbooks.stream()
                .map(GuestbookBoundResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
}