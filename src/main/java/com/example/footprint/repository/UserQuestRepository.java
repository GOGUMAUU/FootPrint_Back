package com.example.footprint.repository;

import com.example.footprint.entity.UserQuest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserQuestRepository extends JpaRepository<UserQuest, Long> {

    // 특정 사용자의 특정 도전과제 조회 (기존 기능)
    Optional<UserQuest> findByUser_UserIdAndQuest_QuestId(
            Long userId,
            Long questId
    );

    // 특정 사용자가 완료한 도전과제 목록 조회 (배지 기능)
    List<UserQuest> findByUser_UserIdAndIsCompletedTrueOrderByCompletedAtDesc(
            Long userId
    );

    // 특정 도전과제를 완료한 전체 사용자 수 조회 (신규)
    long countByQuest_QuestIdAndIsCompletedTrue(Long questId);
}
