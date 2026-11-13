package com.example.footprint.security;

import java.time.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Component
public class JwtTokenProvider {
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;
    public static final long ACCESS_SECONDS = 900;
    public JwtTokenProvider(@Value("${auth.jwt-secret}") String secret, Clock clock) {
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(secret); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("AUTH_JWT_SECRET must be Base64"); }
        if (bytes.length < 32) throw new IllegalStateException("AUTH_JWT_SECRET must contain at least 32 random bytes encoded as Base64");
        var key = new SecretKeySpec(bytes, "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var nimbus = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        var timestamp = new JwtTimestampValidator(Duration.ZERO);
        timestamp.setClock(clock);
        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamp,
                new JwtIssuerValidator("footprint"),
                new JwtClaimValidator<List<String>>("aud", a -> a != null && a.contains("footprint-api"))));
        decoder = nimbus;
        this.clock = clock;
    }
    public String issue(Long userId, String sessionId) {
        Instant now = clock.instant();
        var claims = JwtClaimsSet.builder().issuer("footprint").subject(userId.toString())
                .audience(List.of("footprint-api")).issuedAt(now).expiresAt(now.plusSeconds(ACCESS_SECONDS))
                .id(UUID.randomUUID().toString()).claim("sid", sessionId).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
    public Jwt decode(String token) { return decoder.decode(token); }
}
