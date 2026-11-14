
package com.example.footprint.service;

import com.example.footprint.dto.ChallengeSubmissionRequest;
import com.example.footprint.dto.ChallengeSubmissionResponse;
import com.example.footprint.entity.ChallengeSubmission;
import com.example.footprint.entity.Quest;
import com.example.footprint.entity.User;
import com.example.footprint.repository.ChallengeSubmissionRepository;
import com.example.footprint.repository.QuestRepository;
import com.example.footprint.repository.UserRepository;
import com.example.footprint.security.CurrentUserProvider;
import com.example.footprint.util.GeoUtils;
import com.example.footprint.entity.UserQuest;
import com.example.footprint.repository.UserQuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChallengeSubmissionService {

    private final ChallengeSubmissionRepository submissionRepository;
    private final UserQuestRepository userQuestRepository;
    private final QuestRepository questRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PhotoStorageService photoStorageService;

    @Transactional
    public ChallengeSubmissionResponse submit(
            Long challengeId,
            ChallengeSubmissionRequest request
    ) {
        Long userId = currentUserProvider.id();

        UserQuest existing = userQuestRepository
                .findByUser_UserIdAndQuest_QuestId(userId, challengeId)
                .orElse(null);

        if (existing != null && Boolean.TRUE.equals(existing.getIsCompleted())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 완료한 도전과제입니다."
            );
        }

        Quest quest = questRepository.findById(challengeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "도전과제를 찾을 수 없습니다."
                ));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."
                ));

        if (request == null || request.photoUrl() == null
                || request.photoUrl().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "인증 사진이 필요합니다."
            );
        }

        photoStorageService.requireOwned(userId, request.photoUrl());

        double latitude = request.latitude();
        double longitude = request.longitude();

        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "올바르지 않은 GPS 좌표입니다."
            );
        }

        double distance = GeoUtils.distanceMeters(
                latitude,
                longitude,
                quest.getTargetLatitude().doubleValue(),
                quest.getTargetLongitude().doubleValue()
        );

        if (distance > quest.getTargetRadius()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "도전과제 인증 가능 범위를 벗어났습니다."
            );
        }


        if (request.memo() != null && request.memo().length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "메모는 500자 이하로 작성해 주세요."
            );
        }

        ChallengeSubmission submission = new ChallengeSubmission();
        submission.setUser(user);
        submission.setQuest(quest);
        submission.setPhotoUrl(request.photoUrl());
        submission.setMemo(request.memo());
        submission.setStatus("APPROVED");
        submission.setSubmittedAt(LocalDateTime.now());

        ChallengeSubmission saved = submissionRepository.save(submission);

        UserQuest userQuest = existing;

        if (userQuest == null) {
            userQuest = new UserQuest();
            userQuest.setUser(user);
            userQuest.setQuest(quest);
        }

        userQuest.setIsCompleted(true);
        userQuest.setCompletedAt(LocalDateTime.now());

        userQuestRepository.save(userQuest);

        return new ChallengeSubmissionResponse(
                saved.getSubmissionId(),
                quest.getQuestId(),
                saved.getPhotoUrl(),
                saved.getMemo(),
                saved.getStatus(),
                saved.getSubmittedAt()
        );
    }
}
