package com.example.footprint.service;

import com.example.footprint.dto.UserDtos;
import com.example.footprint.entity.User;
import com.example.footprint.exception.ApiException;
import com.example.footprint.repository.UserRepository;
import com.example.footprint.util.PasswordPolicy;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.example.footprint.dto.AuthDtos.*;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final PasswordPolicy passwords;
    private final PolicyService policies;
    private final SessionService sessions;
    private final UserService profiles;
    private final LoginRateLimiter limiter;
    private final String dummyHash;
    private final SecureRandom random = new SecureRandom();
    public AuthService(UserRepository users, PasswordEncoder encoder, PasswordPolicy passwords, PolicyService policies,
                       SessionService sessions, UserService profiles, Clock clock) {
        this.users = users;
        this.encoder = encoder;
        this.passwords = passwords;
        this.policies = policies;
        this.sessions = sessions;
        this.profiles = profiles;
        this.limiter = new LoginRateLimiter(clock);
        this.dummyHash = encoder.encode("unused-login-timing-value");
    }
    @Transactional
    public UserDtos.Profile signup(Signup input) {
        String email = normalizeEmail(input.email());
        if (users.existsByEmail(email)) throw ApiException.conflict("EMAIL_ALREADY_EXISTS");
        passwords.validate(input.password());
        String nickname = input.nickname().trim();
        if (nickname.isEmpty()) throw ApiException.bad("INVALID_NICKNAME", "닉네임을 입력해 주세요.");
        var user = User.builder().email(email).nickname(nickname).passwordHash(encoder.encode(input.password()))
                .friendCode(friendCode()).followerCount(0).followingCount(0).guestbookCount(0).build();
        users.saveAndFlush(user);
        if (input.consents() != null && !input.consents().isEmpty()) {
            policies.record(user.getUserId(), input.consents());
        }
        return profiles.profile(user.getUserId(), user.getUserId());
    }
    @Transactional
    public Tokens login(Login input, String ip) {
        String email = normalizeEmail(input.email());
        limiter.check(email, ip);
        var found = users.findByEmailAndDeletedAtIsNull(email);
        String hash = found.map(User::getPasswordHash).orElse(null);
        boolean valid = encoder.matches(input.password(), hash == null ? dummyHash : hash);
        if (found.isEmpty() || hash == null || !valid) throw ApiException.unauthorized("INVALID_CREDENTIALS");
        // Lock and recheck: a concurrent password change must not issue a new session using the old password.
        var user = users.lockActive(found.get().getUserId()).orElseThrow(() -> ApiException.unauthorized("INVALID_CREDENTIALS"));
        // Refresh the entity after acquiring the lock because it may already be in the persistence context.
        return loginLocked(user, input.password());
    }
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
    private Tokens loginLocked(User user, String password) {
        em.refresh(user);
        if (user.getDeletedAt() != null || user.getPasswordHash() == null || !encoder.matches(password, user.getPasswordHash()))
            throw ApiException.unauthorized("INVALID_CREDENTIALS");
        return sessions.create(user.getUserId());
    }
    private String friendCode() {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int attempt=0; attempt<10; attempt++) {
            StringBuilder code = new StringBuilder();
            for (int i=0;i<10;i++) code.append(alphabet.charAt(random.nextInt(alphabet.length())));
            if (!users.existsByFriendCode(code.toString())) return code.toString();
        }
        throw ApiException.conflict("FRIEND_CODE_UNAVAILABLE");
    }
    public static String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    /** 로그인 요청 제한 상태는 인증 서비스 한 곳에서만 사용한다. */
    private static final class LoginRateLimiter {
        private record Window(Instant until, int count) {}

        private final Map<String, Window> windows = new HashMap<>();
        private final Clock clock;

        private LoginRateLimiter(Clock clock) {
            this.clock = clock;
        }

        public synchronized void check(String email, String ip) {
            Instant now = clock.instant();
            windows.entrySet().removeIf(entry -> !entry.getValue().until().isAfter(now));
            consume("ip:" + ip, 50, now);
            consume("email:" + email, 10, now);
        }

        private void consume(String key, int limit, Instant now) {
            Window old = windows.get(key);
            if ((old != null && old.count() >= limit) || (old == null && windows.size() >= 10000)) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "잠시 후 다시 시도해 주세요.");
            }
            windows.put(key, old == null ? new Window(now.plusSeconds(900), 1)
                    : new Window(old.until(), old.count() + 1));
        }
    }
}
