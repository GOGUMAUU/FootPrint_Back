
package com.example.footprint.service;

import com.example.footprint.dto.ChallengeDetailResponse;
import com.example.footprint.dto.ChallengeListResponse;
import com.example.footprint.entity.Quest;
import com.example.footprint.repository.QuestRepository;
import com.example.footprint.dto.VerifyLocationRequest;
import com.example.footprint.dto.VerifyLocationResponse;
import com.example.footprint.util.GeoUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ChallengeService {

    private final QuestRepository questRepository;

    public ChallengeService(QuestRepository questRepository) {
        this.questRepository = questRepository;
    }

    @Transactional(readOnly = true)
    public ChallengeListResponse getAllChallenges() {
        List<Quest> quests = questRepository.findAll();

        List<ChallengeListResponse.ChallengeItem> items =
                quests.stream()
                        .map(quest -> new ChallengeListResponse.ChallengeItem(
                                quest.getQuestId(),
                                quest.getTitle(),
                                quest.getRegionName(),
                                null,
                                quest.getBadgeImageUrl(),
                                false
                        ))
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
