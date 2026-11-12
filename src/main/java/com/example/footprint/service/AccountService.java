package com.example.footprint.service;

import com.example.footprint.dto.UserDtos;
import com.example.footprint.entity.User;
import com.example.footprint.exception.ApiException;
import com.example.footprint.repository.BlockRepository;
import com.example.footprint.repository.FollowRepository;
import com.example.footprint.repository.UserRepository;
import com.example.footprint.util.PasswordPolicy;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class AccountService {
    private final UserRepository users;
    private final FollowRepository follows;
    private final BlockRepository blocks;
    private final RelationshipService relationships;
    private final PasswordEncoder encoder;
    private final PasswordPolicy passwords;
    private final SessionService sessions;
    private final ProfileImageService images;
    private final Clock clock;
    private final EntityManager em;
    @Transactional
    public void password(Long id,UserDtos.Password request) {
        var user=users.lockActive(id).orElseThrow(ApiException::missing);
        checkPassword(user,request.currentPassword());
        passwords.validate(request.newPassword());
        if(encoder.matches(request.newPassword(),user.getPasswordHash())) throw ApiException.bad("PASSWORD_UNCHANGED","다른 비밀번호를 입력해 주세요.");
        user.setPasswordHash(encoder.encode(request.newPassword()));
        sessions.revokeAll(id);
    }
    @Transactional
    public void delete(Long id,String password) {
        // Lock every affected user in the same order used by follow/block writes.
        var initial=follows.findByFollower_UserIdOrFollowing_UserId(id,id);
        SortedSet<Long> ids=new TreeSet<>(); ids.add(id);
        initial.forEach(f -> {ids.add(f.getFollower().getUserId());ids.add(f.getFollowing().getUserId());});
        // Detach snapshots so post-lock reads see any committed concurrent relationship changes.
        initial.forEach(em::detach);
        for(Long userId:ids) { var u=users.lockAny(userId).orElseThrow(ApiException::missing); em.refresh(u); }
        var user=users.findById(id).orElseThrow(ApiException::missing);
        if(user.getDeletedAt()!=null) throw ApiException.missing();
        checkPassword(user,password);
        var current=follows.findByFollower_UserIdOrFollowing_UserId(id,id);
        if(current.stream().anyMatch(f -> !ids.contains(f.getFollower().getUserId()) || !ids.contains(f.getFollowing().getUserId())))
            throw ApiException.conflict("ACCOUNT_RELATIONS_CHANGED");
        current.forEach(relationships::remove);
        blocks.deleteAll(blocks.findByBlockerIdOrBlockedId(id,id));
        sessions.revokeAll(id);
        images.deleteAfterCommit(user.getProfileImageUrl());
        user.setDeletedAt(LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC));
        user.setEmail("deleted-"+UUID.randomUUID()+"@deleted.invalid");
        user.setNickname("탈퇴 사용자"); user.setPasswordHash(null); user.setFriendCode(null); user.setProfileImageUrl(null);
        user.setPrivateAccount(true); user.setFollowerCount(0); user.setFollowingCount(0);
    }
    private void checkPassword(User user,String password) {
        if(user.getPasswordHash()==null || !encoder.matches(password,user.getPasswordHash())) throw ApiException.unauthorized("INVALID_CREDENTIALS");
    }
}
