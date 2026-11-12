package com.example.footprint.service;

import com.example.footprint.entity.RefreshToken;
import com.example.footprint.entity.Session;
import com.example.footprint.exception.ApiException;
import com.example.footprint.repository.RefreshTokenRepository;
import com.example.footprint.repository.SessionRepository;
import com.example.footprint.repository.UserRepository;
import com.example.footprint.security.JwtTokenProvider;
import jakarta.persistence.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.example.footprint.dto.AuthDtos.*;

@Service @RequiredArgsConstructor
public class SessionService {
    private final SessionRepository sessions;
    private final RefreshTokenRepository refreshTokens;
    private final UserRepository users;
    private final JwtTokenProvider jwt;
    private final Clock clock;
    private final EntityManager em;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public Tokens create(Long userId) {
        users.lockActive(userId).orElseThrow(() -> ApiException.unauthorized("SESSION_REVOKED"));
        var session = new Session(); session.setSessionId(UUID.randomUUID().toString()); session.setUserId(userId);
        session.setCreatedAt(clock.instant()); session.setLastUsedAt(clock.instant()); session.setExpiresAt(clock.instant().plus(Duration.ofDays(14)));
        sessions.save(session);
        return issue(session);
    }
    @Transactional(noRollbackFor=ApiException.class)
    public Tokens refresh(String raw) {
        var token = refreshTokens.findById(hash(raw)).orElseThrow(() -> ApiException.unauthorized("REFRESH_TOKEN_INVALID"));
        var session = sessions.findById(token.getSessionId()).orElseThrow(() -> ApiException.unauthorized("REFRESH_TOKEN_INVALID"));
        users.lockActive(session.getUserId()).orElseThrow(() -> ApiException.unauthorized("SESSION_REVOKED"));
        em.refresh(session, LockModeType.PESSIMISTIC_WRITE);
        em.refresh(token);
        if (session.getRevokedAt() != null) throw ApiException.unauthorized("SESSION_REVOKED");
        if (!session.getExpiresAt().isAfter(clock.instant())) throw ApiException.unauthorized("REFRESH_TOKEN_EXPIRED");
        if (token.getUsedAt() != null) {
            session.setRevokedAt(clock.instant());
            throw ApiException.unauthorized("REFRESH_TOKEN_REUSED");
        }
        token.setUsedAt(clock.instant());
        session.setLastUsedAt(clock.instant());
        return issue(session);
    }
    @Transactional
    public void logout(String raw) {
        var token = refreshTokens.findById(hash(raw)).orElseThrow(() -> ApiException.unauthorized("REFRESH_TOKEN_INVALID"));
        var session = sessions.findById(token.getSessionId()).orElseThrow(() -> ApiException.unauthorized("REFRESH_TOKEN_INVALID"));
        users.lockAny(session.getUserId()).orElseThrow(() -> ApiException.unauthorized("SESSION_REVOKED"));
        em.refresh(session, LockModeType.PESSIMISTIC_WRITE);
        session.setRevokedAt(clock.instant());
    }
    @Transactional
    public void revokeAll(Long userId) {
        users.lockAny(userId).orElseThrow(ApiException::missing);
        sessions.findByUserId(userId).forEach(s -> s.setRevokedAt(clock.instant()));
    }
    private Tokens issue(Session session) {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var token = new RefreshToken(); token.setTokenHash(hash(raw)); token.setSessionId(session.getSessionId()); refreshTokens.save(token);
        return new Tokens(jwt.issue(session.getUserId(), session.getSessionId()), raw, "Bearer", JwtTokenProvider.ACCESS_SECONDS);
    }
    public static String hash(String raw) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
