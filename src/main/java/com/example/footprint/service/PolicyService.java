package com.example.footprint.service;

import com.example.footprint.entity.PolicyDocument;
import com.example.footprint.entity.UserConsent;
import com.example.footprint.exception.ApiException;
import com.example.footprint.repository.ConsentRepository;
import com.example.footprint.repository.PolicyRepository;
import com.example.footprint.repository.UserRepository;
import java.time.Clock;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.example.footprint.dto.PolicyDtos.*;

@Service @RequiredArgsConstructor
public class PolicyService {
    private final PolicyRepository policies;
    private final ConsentRepository consents;
    private final UserRepository users;
    private final Clock clock;
    public List<PolicyDocument> current() {
        Map<String,PolicyDocument> latest = new LinkedHashMap<>();
        policies.findByPublishedTrueOrderByPublishedAtDescPolicyIdDesc().stream()
            .filter(p -> !p.getPublishedAt().isAfter(clock.instant())).forEach(p -> latest.putIfAbsent(p.getType(), p));
        return List.copyOf(latest.values());
    }
    public PolicyDocument policy(String type) {
        return current().stream().filter(p -> p.getType().equals(type)).findFirst().orElseThrow(ApiException::missing);
    }
    private void validate(List<ConsentInput> inputs) {
        if (inputs == null) throw ApiException.bad("INVALID_CONSENTS", "동의 목록이 필요합니다.");
        Set<String> seen = new HashSet<>();
        for (var i : inputs) {
            if (i == null || i.type() == null || !seen.add(i.type())) throw ApiException.bad("INVALID_CONSENTS", "중복 또는 잘못된 동의 유형입니다.");
            var p = policy(i.type());
            if (!p.isConsentRequired()) throw ApiException.bad("CONSENT_NOT_APPLICABLE", "열람용 문서는 동의 대상이 아닙니다.");
            if (!p.getVersion().equals(i.version())) throw ApiException.conflict("POLICY_VERSION_MISMATCH");
        }
    }
    @Transactional
    public void record(Long userId, List<ConsentInput> inputs) {
        users.lockActive(userId).orElseThrow(ApiException::missing);
        validate(inputs);
        for (var i : inputs) {
            if (!Boolean.TRUE.equals(i.accepted())) continue;
            if (consents.existsByUserIdAndTypeAndVersionAndWithdrawnAtIsNull(userId, i.type(), i.version())) continue;
            var c = new UserConsent(); c.setUserId(userId); c.setType(i.type()); c.setVersion(i.version()); c.setConsentedAt(clock.instant());
            consents.save(c);
        }
    }
    @Transactional
    public void withdraw(Long userId, String type) {
        users.lockActive(userId).orElseThrow(ApiException::missing);
        if (!policy(type).isWithdrawable()) throw ApiException.bad("CONSENT_NOT_WITHDRAWABLE", "이 동의는 해당 API로 철회할 수 없습니다.");
        consents.findByUserIdAndTypeAndWithdrawnAtIsNull(userId, type).forEach(c -> c.setWithdrawnAt(clock.instant()));
    }
    public void requireLocation(Long userId) {
        var p = current().stream().filter(d -> "LOCATION".equals(d.getType()) && d.isConsentRequired()).findFirst();
        if (p.isEmpty() || !consents.existsByUserIdAndTypeAndVersionAndWithdrawnAtIsNull(userId, "LOCATION", p.get().getVersion()))
            throw new ApiException(HttpStatus.FORBIDDEN, "LOCATION_CONSENT_REQUIRED", "위치기반서비스 동의가 필요합니다.");
    }
}
