package com.example.footprint.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.footprint.dto.GuestbookRequest;
import com.example.footprint.dto.GuestbookResponse;
import com.example.footprint.dto.ReportRequest;
import com.example.footprint.entity.Guestbook;
import com.example.footprint.entity.Guestbook.Visibility;
import com.example.footprint.entity.GuestbookLike;
import com.example.footprint.entity.GuestbookPhoto;
import com.example.footprint.entity.GuestbookView;
import com.example.footprint.entity.Report;
import com.example.footprint.entity.Report.ReportReason;
import com.example.footprint.entity.Report.ReportStatus;
import com.example.footprint.entity.User;
import com.example.footprint.repository.GuestbookLikeRepository;
import com.example.footprint.repository.GuestbookPhotoRepository;
import com.example.footprint.repository.GuestbookRepository;
import com.example.footprint.repository.GuestbookViewRepository;
import com.example.footprint.repository.ReportRepository;
import com.example.footprint.repository.UserRepository;
import com.example.footprint.util.GeoUtils;
import com.example.footprint.util.GuestbookPolicy;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GuestbookService {

    private final GuestbookRepository guestbookRepository;
    private final GuestbookPhotoRepository photoRepository;
    private final GuestbookLikeRepository likeRepository;
    private final GuestbookViewRepository viewRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PhotoStorageService storage;

    // ---------- 작성 ----------
    @Transactional
    public GuestbookResponse create(Long userId, GuestbookRequest req) {
        User user = requireUser(userId);

        if (req.getLatitude() == null || req.getLongitude() == null) {
            throw bad("방명록 위치가 필요합니다.");
        }
        if (req.getCurrentLatitude() == null || req.getCurrentLongitude() == null) {
            throw bad("현재 위치가 필요합니다.");
        }
        validateKorea(req.getLatitude(), req.getLongitude());

        String content = validateContent(req.getContent());
        List<String> photoPaths = validatePhotos(req.getPhotoPaths());

        double distance = GeoUtils.distanceMeters(req.getCurrentLatitude(), req.getCurrentLongitude(),
                req.getLatitude(), req.getLongitude());
        if (distance > GuestbookPolicy.CREATE_ALLOWED_RADIUS_METERS) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "현재 위치에서 " + (int) GuestbookPolicy.CREATE_ALLOWED_RADIUS_METERS + "m 이내 장소에만 작성할 수 있어요.");
        }

        Guestbook saved = guestbookRepository.save(Guestbook.builder()
                .user(user)
                .content(content)
                .latitude(toDecimal(req.getLatitude()))
                .longitude(toDecimal(req.getLongitude()))
                .visibility(parseVisibility(req.getVisibility(), Visibility.PUBLIC))
                .locationName(trimToNull(req.getLocationName(), 100))
                .build());

        savePhotos(saved, user, photoPaths);
        user.setGuestbookCount(nz(user.getGuestbookCount()) + 1);
        return toResponse(saved, userId);
    }

    // ---------- 상세 ----------
    @Transactional(readOnly = true)
    public GuestbookResponse get(Long userId, Long guestbookId, Double lat, Double lng) {
        Guestbook g = requireGuestbook(guestbookId);
        boolean mine = g.getUser().getUserId().equals(userId);

        if (!mine) {
            // TODO: FOLLOWERS_ONLY는 팔로우 기능(3번 담당)이 생기면 팔로워에게 허용
            if (g.getVisibility() != null && g.getVisibility() != Visibility.PUBLIC) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "공개되지 않은 방명록입니다.");
            }
            if (lat == null || lng == null) {
                throw bad("현재 위치가 필요합니다.");
            }
            double distance = GeoUtils.distanceMeters(lat, lng,
                    g.getLatitude().doubleValue(), g.getLongitude().doubleValue());
            if (distance > GuestbookPolicy.UNLOCK_RADIUS_METERS) {
                int more = (int) Math.ceil(distance - GuestbookPolicy.UNLOCK_RADIUS_METERS);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, more + "m 더 가까이 가야 볼 수 있어요.");
            }
        }
        return toResponse(g, userId);
    }

    // ---------- 수정 (위치는 변경 불가) ----------
    @Transactional
    public GuestbookResponse update(Long userId, Long guestbookId, GuestbookRequest req) {
        User user = requireUser(userId);
        Guestbook g = requireGuestbook(guestbookId);
        requireOwner(g, userId);

        if (req.getContent() != null) {
            g.setContent(validateContent(req.getContent()));
        }
        if (req.getLocationName() != null) {
            g.setLocationName(trimToNull(req.getLocationName(), 100));
        }
        if (req.getVisibility() != null) {
            g.setVisibility(parseVisibility(req.getVisibility(), g.getVisibility()));
        }
        if (req.getPhotoPaths() != null) {
            List<String> newPaths = validatePhotos(req.getPhotoPaths());
            List<GuestbookPhoto> old = photoRepository.findByGuestbook_GuestbookIdOrderBySortOrderAsc(guestbookId);
            Set<String> keep = new HashSet<>(newPaths);

            photoRepository.deleteAll(old);
            photoRepository.flush();
            savePhotos(g, user, newPaths);

            for (GuestbookPhoto p : old) {
                if (!keep.contains(p.getFilePath())) storage.delete(p.getFilePath());
            }
        }
        return toResponse(g, userId);
    }

    // ---------- 삭제 ----------
    @Transactional
    public void delete(Long userId, Long guestbookId) {
        Guestbook g = requireGuestbook(guestbookId);
        requireOwner(g, userId);

        List<GuestbookPhoto> photos = photoRepository.findByGuestbook_GuestbookIdOrderBySortOrderAsc(guestbookId);

        reportRepository.deleteByGuestbook_GuestbookId(guestbookId);
        likeRepository.deleteByGuestbook_GuestbookId(guestbookId);
        viewRepository.deleteByGuestbook_GuestbookId(guestbookId);
        photoRepository.deleteAll(photos);
        guestbookRepository.delete(g);

        User owner = g.getUser();
        owner.setGuestbookCount(Math.max(0, nz(owner.getGuestbookCount()) - 1));

        for (GuestbookPhoto p : photos) storage.delete(p.getFilePath());
    }

    // ---------- 좋아요 ----------
    @Transactional
    public void like(Long userId, Long guestbookId) {
        User user = requireUser(userId);
        Guestbook g = requireGuestbook(guestbookId);
        if (!likeRepository.existsByGuestbook_GuestbookIdAndUser_UserId(guestbookId, userId)) {
            likeRepository.save(GuestbookLike.builder().guestbook(g).user(user).build());
        }
    }

    @Transactional
    public void unlike(Long userId, Long guestbookId) {
        requireGuestbook(guestbookId);
        likeRepository.deleteByGuestbook_GuestbookIdAndUser_UserId(guestbookId, userId);
    }

    // ---------- 열람 기록 (사용자당 1회) ----------
    @Transactional
    public void saveView(Long userId, Long guestbookId) {
        User user = requireUser(userId);
        Guestbook g = requireGuestbook(guestbookId);
        if (!viewRepository.existsByGuestbook_GuestbookIdAndUser_UserId(guestbookId, userId)) {
            viewRepository.save(GuestbookView.builder().guestbook(g).user(user).build());
        }
    }

    // ---------- 신고 ----------
    @Transactional
    public void report(Long userId, Long guestbookId, ReportRequest req) {
        User reporter = requireUser(userId);
        Guestbook g = requireGuestbook(guestbookId);

        if (g.getUser().getUserId().equals(userId)) {
            throw bad("내 방명록은 신고할 수 없어요.");
        }
        if (reportRepository.existsByGuestbook_GuestbookIdAndReporter_UserId(guestbookId, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 신고한 방명록이에요.");
        }
        ReportReason reason;
        try {
            reason = ReportReason.valueOf(req.getReason() == null ? "" : req.getReason().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw bad("신고 사유가 올바르지 않습니다.");
        }
        reportRepository.save(Report.builder()
                .guestbook(g)
                .reporter(reporter)
                .reason(reason)
                .description(trimToNull(req.getDescription(), 500))
                .status(ReportStatus.PENDING)
                .build());
    }

    // ---------- 내부 헬퍼 ----------
    private void savePhotos(Guestbook g, User uploader, List<String> paths) {
        int order = 0;
        for (String path : paths) {
            photoRepository.save(GuestbookPhoto.builder()
                    .guestbook(g).uploader(uploader).filePath(path).sortOrder(order++).build());
        }
    }

    private GuestbookResponse toResponse(Guestbook g, Long viewerId) {
        Long id = g.getGuestbookId();
        List<String> urls = photoRepository.findByGuestbook_GuestbookIdOrderBySortOrderAsc(id)
                .stream().map(GuestbookPhoto::getFilePath).toList();
        return GuestbookResponse.builder()
                .guestbookId(id)
                .userId(g.getUser().getUserId())
                .nickname(g.getUser().getNickname())
                .content(g.getContent())
                .latitude(g.getLatitude().doubleValue())
                .longitude(g.getLongitude().doubleValue())
                .visibility(g.getVisibility() == null ? Visibility.PUBLIC.name() : g.getVisibility().name())
                .locationName(g.getLocationName())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .photoUrls(urls)
                .likeCount(likeRepository.countByGuestbook_GuestbookId(id))
                .liked(viewerId != null
                        && likeRepository.existsByGuestbook_GuestbookIdAndUser_UserId(id, viewerId))
                .mine(g.getUser().getUserId().equals(viewerId))
                .build();
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
    }

    private Guestbook requireGuestbook(Long id) {
        return guestbookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "방명록을 찾을 수 없습니다."));
    }

    private void requireOwner(Guestbook g, Long userId) {
        if (!g.getUser().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 방명록만 수정/삭제할 수 있습니다.");
        }
    }

    private String validateContent(String content) {
        String c = content == null ? "" : content.trim();
        if (c.isEmpty()) throw bad("글을 입력하세요.");
        if (c.length() > GuestbookPolicy.CONTENT_MAX) {
            throw bad("글은 " + GuestbookPolicy.CONTENT_MAX + "자 이하로 작성하세요.");
        }
        return c;
    }

    private List<String> validatePhotos(List<String> paths) {
        if (paths == null || paths.isEmpty()) throw bad("사진을 1장 이상 추가하세요.");
        if (paths.size() > GuestbookPolicy.MAX_PHOTOS) {
            throw bad("사진은 최대 " + GuestbookPolicy.MAX_PHOTOS + "장까지 가능합니다.");
        }
        for (String p : paths) {
            if (!storage.exists(p)) throw bad("올바르지 않은 사진 경로입니다.");
        }
        return paths;
    }

    private void validateKorea(double lat, double lng) {
        if (lat < GuestbookPolicy.LAT_MIN || lat > GuestbookPolicy.LAT_MAX
                || lng < GuestbookPolicy.LNG_MIN || lng > GuestbookPolicy.LNG_MAX) {
            throw bad("국내 위치에만 작성할 수 있습니다.");
        }
    }

    private Visibility parseVisibility(String value, Visibility fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Visibility.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw bad("공개 범위 값이 올바르지 않습니다.");
        }
    }

    private BigDecimal toDecimal(double v) {
        return BigDecimal.valueOf(v).setScale(8, RoundingMode.HALF_UP);
    }

    private String trimToNull(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return t.length() > max ? t.substring(0, max) : t;
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}