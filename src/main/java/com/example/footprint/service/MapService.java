package com.example.footprint.service;

import com.example.footprint.dto.GuestbookBoundResponseDto;
import com.example.footprint.entity.Guestbook;
import com.example.footprint.exception.ApiException;
import com.example.footprint.repository.FollowRepository;
import com.example.footprint.repository.GuestbookRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapService {

    private final GuestbookRepository guestbookRepository;
    private final FollowRepository followRepository;
    private final UserService profiles;

    public List<GuestbookBoundResponseDto> getGuestbooksInBound(
            Long currentUserId, String scope, Long targetUserId, Double zoom,
            Double southLat, Double westLng, Double northLat, Double eastLng,
            String period, String ageGroup, String sort
    ) {
        if (zoom == null || southLat == null || northLat == null || westLng == null || eastLng == null
                || !Double.isFinite(zoom) || !Double.isFinite(southLat) || !Double.isFinite(northLat)
                || !Double.isFinite(westLng) || !Double.isFinite(eastLng)
                || southLat < -90 || northLat > 90 || westLng < -180 || eastLng > 180
                || southLat > northLat || westLng > eastLng) {
            throw ApiException.bad("INVALID_BOUNDS", "지도 범위가 올바르지 않습니다.");
        }
        List<Long> targetUserIds;

        if ("me".equals(scope)) {
            targetUserIds = Collections.singletonList(currentUserId);
        } else if ("following".equals(scope)) {
            // ACCEPTED 상태의 팔로잉 대상 유저 ID 목록 반환[cite: 3, 4]
            targetUserIds = followRepository.findAcceptedFollowingIds(currentUserId);
        } else if ("user".equals(scope) && targetUserId != null) {
            profiles.requireContentAccess(currentUserId, targetUserId);
            // 특정 유저 조회 시 팔로우 승인 여부 검증[cite: 3, 4]
            boolean isAccepted = followRepository.existsByFollower_UserIdAndFollowing_UserIdAndStatus(currentUserId, targetUserId, "ACCEPTED");
            if (!isAccepted && !currentUserId.equals(targetUserId)) {
                throw ApiException.forbidden();
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
        guestbooks = guestbooks.stream().filter(g -> profiles.canReadGuestbook(currentUserId, g)).collect(Collectors.toList());
        // 일부 대표 기록(예: 상위 N개 또는 특정 조건)만 필터링해서 전달
        if (zoom < 10.0 && guestbooks.size() > 20) {
            guestbooks = guestbooks.subList(0, 20); // 상위 20개만 대표 기록으로 사용
        }

        return guestbooks.stream()
                .map(GuestbookBoundResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
}
