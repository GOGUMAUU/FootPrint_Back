
package com.example.footprint.controller;

import com.example.footprint.dto.ChallengeDetailResponse;
import com.example.footprint.dto.ChallengeListResponse;
import com.example.footprint.service.ChallengeService;
import com.example.footprint.dto.VerifyLocationRequest;
import com.example.footprint.dto.VerifyLocationResponse;
import com.example.footprint.dto.ChallengeSubmissionRequest;
import com.example.footprint.dto.ChallengeSubmissionResponse;
import com.example.footprint.service.ChallengeSubmissionService;
import com.example.footprint.entity.QuestType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

    private final ChallengeService challengeService;
    private final ChallengeSubmissionService submissionService;

    public ChallengeController(
            ChallengeService challengeService,
            ChallengeSubmissionService submissionService
    ) {
        this.challengeService = challengeService;
        this.submissionService = submissionService;
    }
    @GetMapping
    public ChallengeListResponse getChallenges(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) QuestType questType,
            @RequestParam(required = false) Boolean isCompleted,
            @RequestParam(required = false) String keyword
    ) {
        return challengeService.getAllChallenges(
                category,
                region,
                questType,
                isCompleted,
                keyword
        );
    }
    @GetMapping("/{challengeId}")
    public ChallengeDetailResponse getChallengeDetail(
            @PathVariable Long challengeId) {
        return challengeService.getChallengeDetail(challengeId);
    }
    @PostMapping("/{challengeId}/verify-location")
    public VerifyLocationResponse verifyLocation(
            @PathVariable Long challengeId,
            @RequestBody VerifyLocationRequest request
    ) {
        return challengeService.verifyLocation(challengeId, request);
    }
    @PostMapping("/{challengeId}/submissions")
    public ChallengeSubmissionResponse submitChallenge(
            @PathVariable Long challengeId,
            @RequestBody ChallengeSubmissionRequest request
    ) {
        return submissionService.submit(challengeId, request);
    }
}
