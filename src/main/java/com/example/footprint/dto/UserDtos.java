package com.example.footprint.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public final class UserDtos {
    private UserDtos() {}
    public record Summary(Long userId, String nickname, String friendCode, String profileImageUrl, boolean following) {}
    public record Relationship(boolean isSelf, String outgoingFollowStatus, String incomingFollowStatus, boolean blockedByMe) {}
    public record Profile(Long userId, @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) String email, String nickname, String friendCode, String profileImageUrl,
                          boolean isPrivate, int followerCount, int followingCount, int guestbookCount, Relationship relationship) {}
    public record Patch(@Size(min=1,max=50) String nickname, Boolean isPrivate) {}
    public record Password(@NotBlank @Size(max=128) String currentPassword, @NotBlank @Size(max=128) String newPassword) {}
    public record DeleteAccount(@NotBlank @Size(max=128) String currentPassword) {}
    public record Image(String profileImageUrl) {}
    public record FollowResult(Long followId, String status) {}
    public record FollowRequest(Long followId, Summary requester, LocalDateTime createdAt) {}
}
