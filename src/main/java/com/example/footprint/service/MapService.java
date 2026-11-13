package com.example.footprint.service;

import java.math.BigDecimal;
import java.net.URI;
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
import com.example.footprint.repository.maps.GuestbookRepository;

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
    private static final int CLUSTER_MAX_ZOOM = 20;          // 이 줌부터는 안 묶음 (사실상 최대 확대)
    private static final double BUBBLE_W_PX = 160;           // 말풍선 가로(dp) — 앱 GuestbookMarkerRenderer 크기 기준
    private static final double BUBBLE_H_PX = 70;            // 말풍선 세로(dp, 꼬리 포함)
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

    // ───────────────────────── 클러스터링 (말풍선 크기 기준) ─────────────────────────

    /**
     * 화면에서 말풍선끼리 겹치면 하나로 묶는다 (탐욕적 클러스터링).
     *  1) sort 기준으로 정렬 (거리순: 가까운 글 / 인기순: 좋아요 / 최신순)
     *  2) 앞에서부터 하나씩: 이미 뽑힌 대표 말풍선과 화면상 겹치면 그 대표에 합류, 아니면 새 대표가 됨
     * → 겹치는 말풍선이 없어지고, 대표는 항상 그 무리에서 sort 1등.
     *   줌인하면 화면상 거리가 벌어져서 대표부터 순서대로 풀려나온다.
     */
    private List<GuestbookItem> clusterAndPick(List<Candidate> candidates, int zoom, String sort) {
        Comparator<Candidate> cmp = comparatorFor(sort);
        List<Candidate> sorted = candidates.stream().sorted(cmp).collect(Collectors.toList());

        if (zoom >= CLUSTER_MAX_ZOOM || sorted.isEmpty()) {
            return sorted.stream().limit(MAX_ITEMS).map(c -> toItem(c, 1)).collect(Collectors.toList());
        }

        // 줌 레벨 → 1px(dp)당 미터 (웹 메르카토르 기준, 카카오맵 줌 레벨과 거의 같음)
        double refLat = sorted.get(0).g().getLatitude().doubleValue();
        double metersPerPx = 156543.03392 * Math.cos(Math.toRadians(refLat)) / Math.pow(2, zoom);
        double boxW = BUBBLE_W_PX * metersPerPx;   // 말풍선 가로가 차지하는 실제 거리(m)
        double boxH = BUBBLE_H_PX * metersPerPx;   // 말풍선 세로(꼬리 포함)
        double mPerDegLat = 111_320.0;
        double mPerDegLng = 111_320.0 * Math.cos(Math.toRadians(refLat));

        List<Candidate> reps = new ArrayList<>();
        Map<Candidate, Integer> sizes = new HashMap<>();
        for (Candidate c : sorted) {
            double lat = c.g().getLatitude().doubleValue();
            double lng = c.g().getLongitude().doubleValue();
            Candidate joined = null;
            for (Candidate r : reps) {
                double dy = Math.abs(lat - r.g().getLatitude().doubleValue()) * mPerDegLat;
                double dx = Math.abs(lng - r.g().getLongitude().doubleValue()) * mPerDegLng;
                if (dx < boxW && dy < boxH) { joined = r; break; }
            }
            if (joined != null) {
                sizes.merge(joined, 1, Integer::sum);
            } else {
                reps.add(c);
                sizes.put(c, 1);
            }
        }

        return reps.stream()
                .limit(MAX_ITEMS)
                .map(r -> toItem(r, sizes.get(r)))
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