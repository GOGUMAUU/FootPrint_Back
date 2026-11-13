package com.example.footprint.service;


import com.example.footprint.dto.CommonDtos.PageResponse;
import com.example.footprint.entity.Guestbook;
import com.example.footprint.entity.User;
import com.example.footprint.exception.ApiException;
import com.example.footprint.repository.BlockRepository;
import com.example.footprint.repository.FollowRepository;
import com.example.footprint.repository.GuestbookRepository;
import com.example.footprint.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.example.footprint.dto.UserDtos.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository users;
    private final FollowRepository follows;
    private final BlockRepository blocks;
    private final GuestbookRepository guestbooks;

    public List<Guestbook> getMyGuestbooks(Long userId) {
        return guestbooks.findByUser_UserId(userId);
    }

    public User active(Long id) {
        return users.findByUserIdAndDeletedAtIsNull(id).orElseThrow(ApiException::missing);
    }

    public void requireVisible(Long viewer, Long target) {
        active(target);
        if (!viewer.equals(target) && blocks.between(viewer, target)) {
            throw ApiException.missing();
        }
    }

    public void requireContentAccess(Long viewer, Long target) {
        requireVisible(viewer, target);
        if (!viewer.equals(target) && active(target).isPrivateAccount() && !following(viewer, target)) {
            throw ApiException.forbidden();
        }
    }

    public boolean following(Long viewer, Long target) {
        return follows.existsByFollower_UserIdAndFollowing_UserIdAndStatus(viewer, target, "ACCEPTED");
    }

    public boolean canAccessUserGuestbooks(Long viewer, Long target) {
        return viewer.equals(target) || following(viewer, target);
    }

    public boolean canReadGuestbook(Long viewer, Guestbook book) {
        var owner = book.getUser();
        if (owner.getDeletedAt() != null) return false;
        if (owner.getUserId().equals(viewer)) return true;
        if (blocks.between(viewer, owner.getUserId())) return false;
        boolean accepted = following(viewer, owner.getUserId());
        if (owner.isPrivateAccount() && !accepted) return false;
        return book.getVisibility() == null || book.getVisibility() == Guestbook.Visibility.PUBLIC
                || (book.getVisibility() == Guestbook.Visibility.FOLLOWERS_ONLY && accepted);
    }

    public void requireReadableGuestbook(Long viewer, Guestbook book) {
        if (!canReadGuestbook(viewer, book)) throw ApiException.missing();
    }

    public Summary summary(Long viewer, User user) {
        return new Summary(user.getUserId(), user.getNickname(), user.getFriendCode(),
                user.getProfileImageUrl(), following(viewer, user.getUserId()));
    }

    public Profile profile(Long viewer, Long target) {
        requireVisible(viewer, target);
        var user = active(target);
        var relationship = new Relationship(viewer.equals(target), status(viewer, target),
                status(target, viewer), blocks.existsByBlockerIdAndBlockedId(viewer, target));
        return new Profile(user.getUserId(), viewer.equals(target) ? user.getEmail() : null,
                user.getNickname(), user.getFriendCode(), user.getProfileImageUrl(),
                user.isPrivateAccount(), nz(user.getFollowerCount()), nz(user.getFollowingCount()),
                nz(user.getGuestbookCount()), relationship);
    }

    private String status(Long from, Long to) {
        return follows.findByFollower_UserIdAndFollowing_UserId(from, to)
                .map(f -> "REJECTED".equals(f.getStatus()) ? "NONE" : f.getStatus()).orElse("NONE");
    }

    @Transactional
    public Profile patch(Long viewer, Patch request) {
        var user = users.lockActive(viewer).orElseThrow(ApiException::missing);
        if (request.nickname() == null && request.isPrivate() == null) {
            throw ApiException.bad("EMPTY_PATCH", "수정할 항목이 없습니다.");
        }
        if (request.nickname() != null) {
            String nickname = request.nickname().trim();
            if (nickname.isEmpty()) throw ApiException.bad("INVALID_NICKNAME", "닉네임을 입력해 주세요.");
            user.setNickname(nickname);
        }
        if (request.isPrivate() != null) user.setPrivateAccount(request.isPrivate());
        return profile(viewer, viewer);
    }

    public PageResponse<Summary> search(Long viewer, String query, String type, int page, int size) {
        var pageable = PageResponse.request(page, size);
        if (query == null || query.isBlank() || query.length() > 100) {
            throw ApiException.bad("INVALID_QUERY", "검색어는 1~100자여야 합니다.");
        }
        if (type != null && !type.equals("NICKNAME") && !type.equals("USER_CODE")) {
            throw ApiException.bad("INVALID_SEARCH_TYPE", "검색 유형이 올바르지 않습니다.");
        }
        String escaped = query.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return PageResponse.from(users.search(viewer, type == null || type.equals("NICKNAME"),
                type == null || type.equals("USER_CODE"), "%" + escaped + "%",
                query.trim().toUpperCase(Locale.ROOT), pageable), user -> summary(viewer, user));
    }

    public static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
