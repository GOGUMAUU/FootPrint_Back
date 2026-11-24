
package com.example.footprint.service;

import com.example.footprint.dto.ChallengeDetailResponse;
import com.example.footprint.dto.ChallengeListResponse;
import com.example.footprint.entity.Quest;
import com.example.footprint.repository.QuestRepository;
import com.example.footprint.dto.VerifyLocationRequest;
import com.example.footprint.dto.VerifyLocationResponse;
import com.example.footprint.util.GeoUtils;
import com.example.footprint.repository.UserQuestRepository;
import com.example.footprint.security.CurrentUserProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.footprint.repository.FollowRepository;
import com.example.footprint.entity.QuestType;
import com.example.footprint.repository.BlockRepository;
import com.example.footprint.entity.User;
import java.util.Set;
import java.util.HashSet;
import java.util.Locale;
import java.util.List;

@Service
public class ChallengeService {


    private final QuestRepository questRepository;
    private final UserQuestRepository userQuestRepository;
    private final CurrentUserProvider currentUserProvider;
    private final FollowRepository followRepository;
    private final BlockRepository blockRepository;
    public ChallengeService(
            QuestRepository questRepository,
            UserQuestRepository userQuestRepository,
            CurrentUserProvider currentUserProvider,
            FollowRepository followRepository,
            BlockRepository blockRepository
    ) {
        this.questRepository = questRepository;
        this.userQuestRepository = userQuestRepository;
        this.currentUserProvider = currentUserProvider;
        this.followRepository = followRepository;
        this.blockRepository = blockRepository;
    }

    @Transactional(readOnly = true)
    public ChallengeListResponse getAllChallenges() {
        return getAllChallenges(null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public ChallengeListResponse getAllChallenges(
            String category,
            String region,
            QuestType questType,
            Boolean isCompleted,
            String keyword
    ) {
        Long userId = currentUserProvider.id();

        // 현재 사용자가 팔로우하는 사용자 ID 목록
        Set<Long> followingIds = new HashSet<>(
                followRepository.findAcceptedFollowingIds(userId)
        );

        List<Quest> quests = questRepository.findAll();

        List<ChallengeListResponse.ChallengeItem> items =
                quests.stream()

                        // 지역 필터
                        .filter(quest ->
                                region == null
                                        || region.isBlank()
                                        || region.equals(quest.getRegionName())
                        )

                        // 도전과제 유형 필터
                        .filter(quest ->
                                questType == null
                                        || questType == quest.getQuestType()
                        )

                        // 제목 검색
                        .filter(quest ->
                                keyword == null
                                        || keyword.isBlank()
                                        || quest.getTitle()
                                        .toLowerCase(Locale.ROOT)
                                        .contains(keyword.trim()
                                                .toLowerCase(Locale.ROOT))
                        )

                        // 도전과제 조회 권한 확인
                        .filter(quest -> {
                            User creator = quest.getCreator();

                            // 기본 도전과제
                            if (creator == null) {
                                return true;
                            }

                            Long creatorId = creator.getUserId();

                            // 탈퇴한 사용자의 도전과제 제외
                            if (creator.getDeletedAt() != null) {
                                return false;
                            }

                            // 내가 만든 도전과제
                            if (userId.equals(creatorId)) {
                                return true;
                            }

                            // 서로 차단 관계인 경우 제외
                            if (blockRepository.between(userId, creatorId)) {
                                return false;
                            }

                            // 비공개 계정은 승인된 팔로워만 조회 가능
                            if (creator.isPrivateAccount()) {
                                return followingIds.contains(creatorId);
                            }

                            // 공개 계정은 조회 가능
                            return true;
                        })

                        .map(quest -> {

                            Long creatorId =
                                    quest.getCreator() == null
                                            ? null
                                            : quest.getCreator().getUserId();

                            // 도전과제 카테고리 구분
                            String questCategory;

                            if (creatorId == null) {
                                questCategory = "DEFAULT";
                            } else if (creatorId.equals(userId)) {
                                questCategory = "MINE";
                            } else if (followingIds.contains(creatorId)) {
                                questCategory = "FOLLOWING";
                            } else {
                                questCategory = "OTHER";
                            }

                            // 현재 사용자의 달성 여부
                            boolean completed =
                                    userQuestRepository
                                            .findByUser_UserIdAndQuest_QuestId(
                                                    userId,
                                                    quest.getQuestId()
                                            )
                                            .map(userQuest ->
                                                    Boolean.TRUE.equals(
                                                            userQuest.getIsCompleted()
                                                    )
                                            )
                                            .orElse(false);

                            // 전체 달성 인원
                            long completedCount =
                                    userQuestRepository
                                            .countByQuest_QuestIdAndIsCompletedTrue(
                                                    quest.getQuestId()
                                            );

                            return new ChallengeListResponse.ChallengeItem(
                                    quest.getQuestId(),
                                    quest.getTitle(),
                                    quest.getRegionName(),
                                    questCategory,
                                    quest.getBadgeImageUrl(),
                                    completed,
                                    quest.getTargetLatitude(),
                                    quest.getTargetLongitude(),
                                    completedCount,
                                    creatorId,
                                    quest.getQuestType()
                            );
                        })

                        // 카테고리 필터
                        .filter(item ->
                                category == null
                                        || category.isBlank()
                                        || category.equalsIgnoreCase("ALL")
                                        || category.equalsIgnoreCase(item.category())
                        )

                        // 달성 상태 필터
                        .filter(item ->
                                isCompleted == null
                                        || isCompleted == item.isCompleted()
                        )

                        .toList();

        return new ChallengeListResponse(items.size(), items);
    }

    @Transactional(readOnly = true)
    public ChallengeDetailResponse getChallengeDetail(Long challengeId) {

        Quest quest = questRepository.findById(challengeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "도전과제를 찾을 수 없습니다."
                ));

        return new ChallengeDetailResponse(
                quest.getQuestId(),
                quest.getTitle(),
                quest.getDescription(),
                quest.getRegionName(),
                quest.getTargetLatitude(),
                quest.getTargetLongitude(),
                quest.getTargetRadius(),
                new ChallengeDetailResponse.RewardBadge(
                        null,
                        null,
                        quest.getBadgeImageUrl()
                ),
                null,
                null
        );
    }

    @Transactional(readOnly = true)
    public VerifyLocationResponse verifyLocation(
            Long challengeId,
            VerifyLocationRequest request
    ) {
        Quest quest = questRepository.findById(challengeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "도전과제를 찾을 수 없습니다."
                ));

        double distance = GeoUtils.distanceMeters(
                request.latitude(),
                request.longitude(),
                quest.getTargetLatitude().doubleValue(),
                quest.getTargetLongitude().doubleValue()
        );

        int radius = quest.getTargetRadius();
        boolean verified = distance <= radius;

        return new VerifyLocationResponse(verified, distance, radius);
    }

}
