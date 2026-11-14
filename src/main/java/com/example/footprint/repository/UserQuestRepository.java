package com.example.footprint.repository;

import com.example.footprint.entity.UserQuest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserQuestRepository
        extends JpaRepository<UserQuest, Long> {

    Optional<UserQuest> findByUser_UserIdAndQuest_QuestId(
            Long userId,
            Long questId
    );
}
