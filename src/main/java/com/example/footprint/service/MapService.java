package com.example.footprint.service;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.footprint.dto.maps.NearbyGuestbookRequestDto;
import com.example.footprint.dto.maps.NearbyGuestbookResponseDto;
import com.example.footprint.dto.maps.NearbyGuestbookResponseDto.GuestbookItem;
import com.example.footprint.dto.maps.PlaceSearchResponseDto;
import com.example.footprint.entity.Guestbook;
import com.example.footprint.exception.ApiException;
import com.example.footprint.repository.FollowRepository;
import com.example.footprint.repository.GuestbookRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapService {

    // ── 정책 값 (앱 AppConfig와 맞출 것) ──
    private static final double NEARBY_RADIUS_KM = 1.0;      // 주변 모드 조회 반경
    private static final double UNLOCK_RADIUS_M = 100.0;     // 열람 가능 거리
    private static final int CLUSTER_MAX_ZOOM = 17;          // 이 줌부터는 낱개로
    private static final double BASE_CELL_DEG = 0.001;       // 줌 16 기준 격자 ≈ 110m (줌 1 내릴 때마다 2배)
    private static final int MAX_ITEMS = 50;                 // 한 번에 내려줄 최대 말풍선 수
    private static final int TITLE_MAX = 30;

    private final GuestbookRepository guestbookRepository;
    private final FollowRepository followRepository;
    private final UserService profiles;                 // 차단·비공개 계정·공개범위 판단 (main)
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    /** 클러스터링·정렬용 내부 후보 */
    private record Candidate(Guestbook g, Double distance, int likes) {}

    // ───────────────────────── 영역(팔로잉/내 지도/특정 유저) ─────────────────────────

    public List<GuestbookItem> getGuestbooksInBound(
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
            targetUserIds = followRepository.findAcceptedFollowingIds(currentUserId);
        } else if ("user".equals(scope) && targetUserId != null) {
            profiles.requireContentAccess(currentUserId, targetUserId);
            boolean isAccepted = followRepository.existsByFollower_UserIdAndFollowing_UserIdAndStatus(currentUserId, targetUserId, "ACCEPTED");
            if (!isAccepted && !currentUserId.equals(targetUserId)) {
                throw ApiException.forbidden();
            }
            targetUserIds = Collections.singletonList(targetUserId);
        } else {
            return Collections.emptyList();
        }
        if (targetUserIds == null || targetUserIds.isEmpty()) return Collections.emptyList();

        List<Guestbook> guestbooks = guestbookRepository.findInBound(
                targetUserIds,
                currentUserId,
                Guestbook.Visibility.PRIVATE,
                BigDecimal.valueOf(southLat),
                BigDecimal.valueOf(northLat),
                BigDecimal.valueOf(westLng),
                BigDecimal.valueOf(eastLng)
        );
        // 차단·비공개 계정·FOLLOWERS_ONLY 등 최종 열람 권한 필터 (main)
        guestbooks = guestbooks.stream()
                .filter(g -> profiles.canReadGuestbook(currentUserId, g))
                .collect(Collectors.toList());
        // 2. 기간(period) 필터 추가 ("7d", "30d")
    if (period != null && !period.isBlank()) {
        LocalDateTime cutoff = null;
    if ("7d".equals(period)) {
        cutoff = LocalDateTime.now().minusDays(7);
    } else if ("30d".equals(period)) {
        cutoff = LocalDateTime.now().minusDays(30);
    }
    if (cutoff != null) {
        final LocalDateTime finalCutoff = cutoff;
        guestbooks = guestbooks.stream()
                .filter(g -> g.getCreatedAt() != null && !g.getCreatedAt().isBefore(finalCutoff))
                .collect(Collectors.toList());
    }
}

        Map<Long, Integer> likes = loadLikeCounts(guestbooks);
        List<Candidate> candidates = new ArrayList<>();
        for (Guestbook g : guestbooks) {
            candidates.add(new Candidate(g, null, likes.getOrDefault(g.getGuestbookId(), 0)));
        }

        // 영역 조회엔 '내 위치'가 없으므로 popular | latest
        String normalizedSort = "popular".equals(sort) ? "popular" : "latest";
        int zoomLevel = (int) Math.floor(zoom);
        return clusterAndPick(candidates, zoomLevel, normalizedSort);
    }

    // ───────────────────────── 주변(내 위치 1km) ─────────────────────────

    public NearbyGuestbookResponseDto getNearbyGuestbooks(NearbyGuestbookRequestDto request, Long currentUserId) {
        if (request.getLatitude() == null || request.getLongitude() == null
                || !Double.isFinite(request.getLatitude()) || !Double.isFinite(request.getLongitude())
                || Math.abs(request.getLatitude()) > 90 || Math.abs(request.getLongitude()) > 180) {
            throw ApiException.bad("INVALID_LOCATION", "위치 정보가 올바르지 않습니다.");
        }
        double myLat = request.getLatitude();
        double myLng = request.getLongitude();
        int zoomLevel = request.getZoomLevel() != null ? request.getZoomLevel() : 16;
        String sort = "popular".equals(request.getSort()) ? "popular" : "distance";

        // 1) 반경을 감싸는 사각형으로 DB에서 후보만 가져오고
        double dLat = NEARBY_RADIUS_KM / 111.32;
        double dLng = dLat / Math.cos(Math.toRadians(myLat));
        List<Guestbook> raw = guestbookRepository.findVisibleInBox(
                currentUserId,
                Guestbook.Visibility.PRIVATE,
                BigDecimal.valueOf(myLat - dLat),
                BigDecimal.valueOf(myLat + dLat),
                BigDecimal.valueOf(myLng - dLng),
                BigDecimal.valueOf(myLng + dLng)
        );

        // 2) 열람 권한 필터 (main) + 정확한 거리로 원형(1km) 필터
        raw = raw.stream()
                .filter(g -> profiles.canReadGuestbook(currentUserId, g))
                .collect(Collectors.toList());
        Map<Long, Integer> likes = loadLikeCounts(raw);
        List<Candidate> candidates = new ArrayList<>();
        for (Guestbook g : raw) {
            double d = calculateDistanceMeter(myLat, myLng,
                    g.getLatitude().doubleValue(), g.getLongitude().doubleValue());
            if (d <= NEARBY_RADIUS_KM * 1000) {
                candidates.add(new Candidate(g, d, likes.getOrDefault(g.getGuestbookId(), 0)));
            }
        }

        // 3) 줌에 맞게 클러스터링 + 정렬
        List<GuestbookItem> items = clusterAndPick(candidates, zoomLevel, sort);

        return NearbyGuestbookResponseDto.builder()
                .centerLatitude(myLat)
                .centerLongitude(myLng)
                .totalCount(items.size())
                .guestbooks(items)
                .build();
    }

    // ───────────────────────── 클러스터링 (격자 방식) ─────────────────────────

    /**
     * 줌 < CLUSTER_MAX_ZOOM 이면 격자 칸 단위로 묶는다. 줌아웃할수록 칸이 2배씩 커짐.
     * 각 칸의 대표 글 = sort 기준 1등 (거리순: 가장 가까운 글 / 인기순: 좋아요 1등 / 최신순: 최신 글)
     * 대표 글 좌표에 말풍선을 찍으므로, 줌인하면 대표 글부터 순서대로 풀려나온다.
     */
    private List<GuestbookItem> clusterAndPick(List<Candidate> candidates, int zoom, String sort) {
        Comparator<Candidate> cmp = comparatorFor(sort);

        if (zoom >= CLUSTER_MAX_ZOOM) {
            return candidates.stream()
                    .sorted(cmp)
                    .limit(MAX_ITEMS)
                    .map(c -> toItem(c, 1))
                    .collect(Collectors.toList());
        }

        double cell = BASE_CELL_DEG * Math.pow(2, (CLUSTER_MAX_ZOOM - 1) - zoom);
        Map<String, List<Candidate>> groups = new LinkedHashMap<>();
        for (Candidate c : candidates) {
            long row = (long) Math.floor(c.g().getLatitude().doubleValue() / cell);
            long col = (long) Math.floor(c.g().getLongitude().doubleValue() / cell);
            groups.computeIfAbsent(row + ":" + col, k -> new ArrayList<>()).add(c);
        }

        List<Candidate> representatives = new ArrayList<>();
        Map<Candidate, Integer> sizes = new HashMap<>();
        for (List<Candidate> group : groups.values()) {
            group.sort(cmp);
            Candidate rep = group.get(0);
            representatives.add(rep);
            sizes.put(rep, group.size());
        }

        return representatives.stream()
                .sorted(cmp)
                .limit(MAX_ITEMS)
                .map(rep -> toItem(rep, sizes.get(rep)))
                .collect(Collectors.toList());
    }

    private Comparator<Candidate> comparatorFor(String sort) {
        Comparator<Candidate> byDistance = Comparator.comparing(
                Candidate::distance, Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<Candidate> byLatest = Comparator.comparing(
                (Candidate c) -> c.g().getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder()));
        Comparator<Candidate> byLikes = Comparator.comparingInt(Candidate::likes).reversed();

        return switch (sort) {
            case "popular" -> byLikes.thenComparing(byDistance).thenComparing(byLatest);
            case "latest" -> byLatest;
            default -> byDistance.thenComparing(byLikes); // distance
        };
    }

    private GuestbookItem toItem(Candidate c, int clusterCount) {
        Guestbook g = c.g();
        Double distance = c.distance() != null ? Math.round(c.distance() * 10.0) / 10.0 : null;
        return GuestbookItem.builder()
                .guestbookId(g.getGuestbookId())
                .userId(g.getUser() != null ? g.getUser().getUserId() : null)
                .title(truncate(g.getContent(), TITLE_MAX))
                .content(g.getContent())
                .thumbnailUrl(null)
                .latitude(g.getLatitude().doubleValue())
                .longitude(g.getLongitude().doubleValue())
                .locationName(g.getLocationName())
                .distanceMeter(distance)
                .isAccessible(c.distance() == null || c.distance() <= UNLOCK_RADIUS_M)
                .likeCount(c.likes())
                .isClustered(clusterCount > 1)
                .clusterCount(clusterCount)
                .build();
    }

    private Map<Long, Integer> loadLikeCounts(List<Guestbook> guestbooks) {
        if (guestbooks.isEmpty()) return Collections.emptyMap();
        List<Long> ids = guestbooks.stream().map(Guestbook::getGuestbookId).collect(Collectors.toList());
        Map<Long, Integer> result = new HashMap<>();
        for (Object[] row : guestbookRepository.countLikesByGuestbookIds(ids)) {
            result.put((Long) row[0], ((Long) row[1]).intValue());
        }
        return result;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    // ───────────────────────── 장소 검색 ─────────────────────────

    public List<PlaceSearchResponseDto> searchPlaces(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        URI uri = UriComponentsBuilder
                .fromUriString("https://dapi.kakao.com/v2/local/search/keyword.json")
                .queryParam("query", keyword)
                .queryParam("size", 15)
                .encode()
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey); // REST API 키
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);
        } catch (RestClientException e) {
            log.warn("카카오 장소 검색 실패: {}", e.getMessage());
            return Collections.emptyList();
        }

        List<PlaceSearchResponseDto> result = new ArrayList<>();
        if (response.getBody() != null && response.getBody().get("documents") instanceof List<?> documents) {
            for (Object item : documents) {
                if (item instanceof Map<?, ?> doc) {
                    result.add(PlaceSearchResponseDto.builder()
                            .placeName(String.valueOf(doc.get("place_name")))
                            .addressName(String.valueOf(doc.get("address_name")))
                            .roadAddressName(String.valueOf(doc.get("road_address_name")))
                            .latitude(Double.parseDouble(String.valueOf(doc.get("y"))))
                            .longitude(Double.parseDouble(String.valueOf(doc.get("x"))))
                            .categoryName(String.valueOf(doc.get("category_group_name")))
                            .build());
                }
            }
        }
        return result;
    }

    // ───────────────────────── 거리 ─────────────────────────

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