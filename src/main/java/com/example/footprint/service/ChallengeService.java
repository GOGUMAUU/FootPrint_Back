
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

import java.util.List;

@Service
public class ChallengeService {


    private final QuestRepository questRepository;
    private final UserQuestRepository userQuestRepository;
    private final CurrentUserProvider currentUserProvider;

    public ChallengeService(
            QuestRepository questRepository,
            UserQuestRepository userQuestRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.questRepository = questRepository;
        this.userQuestRepository = userQuestRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public ChallengeListResponse getAllChallenges() {

        Long userId = currentUserProvider.id();

        List<Quest> quests = questRepository.findAll();

        List<ChallengeListResponse.ChallengeItem> items =
                quests.stream()
                        .map(quest -> {
                            boolean isCompleted = userQuestRepository.findByUser_UserIdAndQuest_QuestId(userId,quest.getQuestId()).map(userQuest -> Boolean.TRUE.equals(userQuest.getIsCompleted())).orElse(false);
                            // 이 도전과제를 완료한 전체 사용자 수
                            long completedCount = userQuestRepository.countByQuest_QuestIdAndIsCompletedTrue(quest.getQuestId());


                            return new ChallengeListResponse.ChallengeItem(
                                    quest.getQuestId(),
                                    quest.getTitle(),
                                    quest.getRegionName(),
                                    null,
                                    quest.getBadgeImageUrl(),
                                    isCompleted,
                                    quest.getTargetLatitude(),
                                    quest.getTargetLongitude(),
                                    completedCount
                            );


                        })
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
