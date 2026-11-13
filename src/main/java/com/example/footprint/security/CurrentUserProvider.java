package com.example.footprint.security;

import com.example.footprint.exception.ApiException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {
    public record Principal(Long userId, String sessionId) {}
    public Principal principal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Principal p))
            throw ApiException.unauthorized("AUTHENTICATION_REQUIRED");
        return p;
    }
    public Long id() { return principal().userId(); }
}
