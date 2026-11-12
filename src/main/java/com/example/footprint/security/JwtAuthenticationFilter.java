package com.example.footprint.security;

import com.example.footprint.dto.CommonDtos.ApiError;
import com.example.footprint.exception.ApiException;
import com.example.footprint.repository.SessionRepository;
import com.example.footprint.repository.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokens;
    private final SessionRepository sessions;
    private final UserRepository users;
    private final ObjectMapper json;
    private final Clock clock;
    public JwtAuthenticationFilter(JwtTokenProvider tokens, SessionRepository sessions, UserRepository users, ObjectMapper json, Clock clock) {
        this.tokens = tokens; this.sessions = sessions; this.users = users; this.json = json; this.clock = clock;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/auth/");
    }
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null) {
            try {
                if (!header.startsWith("Bearer ")) throw ApiException.unauthorized("ACCESS_TOKEN_INVALID");
                Jwt jwt = tokens.decode(header.substring(7));
                Long userId = Long.valueOf(jwt.getSubject());
                String sid = jwt.getClaimAsString("sid");
                if (sid == null || jwt.getExpiresAt() == null) throw ApiException.unauthorized("ACCESS_TOKEN_INVALID");
                var session = sessions.findById(sid).orElseThrow(() -> ApiException.unauthorized("SESSION_REVOKED"));
                if (!session.getUserId().equals(userId) || session.getRevokedAt() != null || !session.getExpiresAt().isAfter(clock.instant())
                        || !users.existsByUserIdAndDeletedAtIsNull(userId))
                    throw ApiException.unauthorized("SESSION_REVOKED");
                var principal = new CurrentUserProvider.Principal(userId, sid);
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
            } catch (JwtException | IllegalArgumentException e) {
                String code = e instanceof JwtValidationException && e.getMessage().contains("expired")
                        ? "ACCESS_TOKEN_EXPIRED" : "ACCESS_TOKEN_INVALID";
                write(res, code); return;
            } catch (ApiException e) { write(res, e.code()); return; }
        }
        chain.doFilter(req, res);
    }
    private void write(HttpServletResponse res, String code) throws IOException {
        SecurityContextHolder.clearContext();
        res.setStatus(401); res.setContentType("application/json"); res.setCharacterEncoding("UTF-8");
        json.writeValue(res.getOutputStream(), new ApiError(code, "인증 정보를 확인해 주세요."));
    }
}
