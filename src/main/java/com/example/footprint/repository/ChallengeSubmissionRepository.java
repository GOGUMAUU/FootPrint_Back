package com.example.footprint.repository;

import com.example.footprint.entity.ChallengeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeSubmissionRepository
        extends JpaRepository<ChallengeSubmission, Long> {
}
