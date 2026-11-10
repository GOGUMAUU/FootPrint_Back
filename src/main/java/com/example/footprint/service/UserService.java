package com.example.footprint.service;

import com.example.footprint.entity.Guestbook;
import com.example.footprint.repository.FollowRepository;
import com.example.footprint.repository.GuestbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final GuestbookRepository guestbookRepository;
    private final FollowRepository followRepository;

    /**
     * 내가 작성한 방명록 목록 조회 (GET /api/users/me/guestbooks)
     */
    public List<Guestbook> getMyGuestbooks(Long currentUserId) {
        return guestbookRepository.findByUserId(currentUserId);
    }

    /**
     * 팔로우 승인 여부 검증 (팔로우 승인된 사용자만 상대방 기록 접근 허용)
     */
    public boolean canAccessUserGuestbooks(Long currentUserId, Long targetUserId) {
        // 본인 기록은 언제나 접근 가능
        if (currentUserId.equals(targetUserId)) {
            return true;
        }

        // DB에서 currentUserId가 targetUserId를 팔로우하고 있으며, 상태가 ACCEPTED인지 확인
        return followRepository.existsByFollowerIdAndFollowingIdAndStatus(currentUserId, targetUserId, "ACCEPTED");
    }
}