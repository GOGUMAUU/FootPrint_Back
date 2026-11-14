
package com.example.footprint.controller;

import com.example.footprint.dto.ChallengeDetailResponse;
import com.example.footprint.dto.ChallengeListResponse;
import com.example.footprint.service.ChallengeService;
import com.example.footprint.dto.VerifyLocationRequest;
import com.example.footprint.dto.VerifyLocationResponse;
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

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @GetMapping
    public ChallengeListResponse getChallenges() {
        return challengeService.getAllChallenges();
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
}
