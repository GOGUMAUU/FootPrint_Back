package com.example.footprint.controller;

import com.example.footprint.entity.Guestbook;
import com.example.footprint.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me/guestbooks")
    public ResponseEntity<List<Guestbook>> getMyGuestbooks() {
        Long currentUserId = 1L; // 인증된 유저 ID
        return ResponseEntity.ok(userService.getMyGuestbooks(currentUserId));
    }
}