package com.example.footprint.service;

import com.example.footprint.dto.BadgeListResponse;
import com.example.footprint.entity.UserQuest;
import com.example.footprint.repository.UserQuestRepository;
import com.example.footprint.security.CurrentUserProvider;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BadgeService {

    private final UserQuestRepository userQuestRepository;
    private final CurrentUserProvider currentUserProvider;

    public BadgeService(
            UserQuestRepository userQuestRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.userQuestRepository = userQuestRepository;
        this.currentUserProvider = currentUserProvider;
    }

    // 현재 사용자가 획득한 배지 목록 조회
    @Transactional(readOnly = true)
    public BadgeListResponse getMyBadges() {

        // 현재 로그인한 사용자 ID
        Long userId = currentUserProvider.id();

        // 완료한 도전과제만 최근 완료 순으로 조회
        List<UserQuest> completedQuests = userQuestRepository
                .findByUser_UserIdAndIsCompletedTrueOrderByCompletedAtDesc(
                        userId
                );

        // 완료한 도전과제를 배지 정보로 변환
        List<BadgeListResponse.BadgeItem> badges = completedQuests.stream()
                .map(userQuest -> new BadgeListResponse.BadgeItem(
                        userQuest.getQuest().getQuestId(),
                        userQuest.getQuest().getTitle(),
                        userQuest.getQuest().getBadgeImageUrl(),
                        userQuest.getCompletedAt()
                ))
                .toList();

        return new BadgeListResponse(
                badges.size(),
                badges
        );
    }
}
