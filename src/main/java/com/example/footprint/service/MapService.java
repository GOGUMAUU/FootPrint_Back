package com.example.footprint.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.footprint.dto.GuestbookBoundResponseDto;
import com.example.footprint.dto.maps.NearbyGuestbookRequestDto;
import com.example.footprint.dto.maps.NearbyGuestbookResponseDto;
import com.example.footprint.entity.Guestbook;
import com.example.footprint.repository.FollowRepository;
import com.example.footprint.repository.maps.GuestbookRepository;

import lombok.RequiredArgsConstructor;

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
            targetUserIds = followRepository.findAcceptedFollowingIds(currentUserId);
        } else if ("user".equals(scope) && targetUserId != null) {
            boolean isAccepted = followRepository.existsByFollower_UserIdAndFollowing_UserIdAndStatus(currentUserId, targetUserId, "ACCEPTED");
            if (!isAccepted && !currentUserId.equals(targetUserId)) {
                throw new IllegalArgumentException("팔로우 승인된 사용자만 기록을 조회할 수 있습니다.");
            }
            targetUserIds = Collections.singletonList(targetUserId);
        } else {
            return Collections.emptyList();
        }

        List<Guestbook> guestbooks = guestbookRepository.findInBound(
                targetUserIds,
                BigDecimal.valueOf(southLat),
                BigDecimal.valueOf(northLat),
                BigDecimal.valueOf(westLng),
                BigDecimal.valueOf(eastLng)
        );

        if (zoom < 10.0 && guestbooks.size() > 20) {
            guestbooks = guestbooks.subList(0, 20);
        }

        return guestbooks.stream()
                .map(GuestbookBoundResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public NearbyGuestbookResponseDto getNearbyGuestbooks(NearbyGuestbookRequestDto request, Long currentUserId) {
        double centerLat = request.getLatitude();
        double centerLng = request.getLongitude();
        int zoomLevel = request.getZoomLevel() != null ? request.getZoomLevel() : 15;

        double radiusKm = calculateRadiusByZoom(zoomLevel);
        List<Guestbook> rawGuestbooks = guestbookRepository.findNearbyGuestbooks(centerLat, centerLng, radiusKm);
        List<NearbyGuestbookResponseDto.GuestbookItem> items = processClusteringAndMapping(rawGuestbooks, centerLat, centerLng, zoomLevel);

        return NearbyGuestbookResponseDto.builder()
                .centerLatitude(centerLat)
                .centerLongitude(centerLng)
                .totalCount(items.size())
                .guestbooks(items)
                .build();
    }

    private double calculateRadiusByZoom(int zoom) {
        if (zoom <= 10) return 10.0;
        if (zoom <= 13) return 5.0;
        if (zoom <= 15) return 2.0;
        return 0.5;
    }
private List<NearbyGuestbookResponseDto.GuestbookItem> processClusteringAndMapping(
            List<Guestbook> list, double centerLat, double centerLng, int zoomLevel) {

        List<NearbyGuestbookResponseDto.GuestbookItem> result = new ArrayList<>();

        for (Guestbook g : list) {
            double lat = g.getLatitude().doubleValue();
            double lng = g.getLongitude().doubleValue();
            double distanceMeter = calculateDistanceMeter(centerLat, centerLng, lat, lng);

            boolean isAccessible = distanceMeter <= 100.0;
            boolean isClustered = false;
            int clusterCount = 1;

            result.add(NearbyGuestbookResponseDto.GuestbookItem.builder()
                    .guestbookId(g.getGuestbookId())
                    .title(g.getContent())           // 엔티티에 title이 없으므로 content로 대체
                    .thumbnailUrl(null)              // 추후 이미지 테이블 연동 전까지 null 처리
                    .latitude(lat)
                    .longitude(lng)
                    .locationName(g.getLocationName())
                    .distanceMeter(Math.round(distanceMeter * 10.0) / 10.0)
                    .isAccessible(isAccessible)
                    .likeCount(0)                    // 추후 좋아요 테이블 연동 전까지 0 처리
                    .isClustered(isClustered)
                    .clusterCount(clusterCount)
                    .build());
        }

        return result;
    }



       private double calculateDistanceMeter(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000;
    }
}