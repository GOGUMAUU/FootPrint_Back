package com.example.footprint.controller;

import com.example.footprint.dto.UserDtos;
import com.example.footprint.service.AuthService;
import com.example.footprint.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import static com.example.footprint.dto.AuthDtos.*;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
@io.swagger.v3.oas.annotations.security.SecurityRequirements
public class AuthController {
    private final AuthService auth;
    private final SessionService sessions;
    @PostMapping("/signup") public ResponseEntity<UserDtos.Profile> signup(@Valid @RequestBody Signup request) {
        return ResponseEntity.status(201).body(auth.signup(request));
    }
    @PostMapping("/login") public ResponseEntity<Tokens> login(@Valid @RequestBody Login request, HttpServletRequest http) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(auth.login(request, http.getRemoteAddr()));
    }
    @PostMapping("/refresh") public ResponseEntity<Tokens> refresh(@Valid @RequestBody Refresh request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(sessions.refresh(request.refreshToken()));
    }
    @PostMapping("/logout") public ResponseEntity<Void> logout(@Valid @RequestBody Refresh request) {
        sessions.logout(request.refreshToken()); return ResponseEntity.noContent().build();
    }
}
