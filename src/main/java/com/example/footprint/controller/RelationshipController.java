package com.example.footprint.controller;

import com.example.footprint.dto.CommonDtos.PageResponse;
import com.example.footprint.security.CurrentUserProvider;
import com.example.footprint.service.RelationshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static com.example.footprint.dto.UserDtos.*;

@RestController @RequiredArgsConstructor
public class RelationshipController {
    private final RelationshipService service;
    private final CurrentUserProvider current;
    @PostMapping("/api/follows/{userId}") public ResponseEntity<FollowResult> follow(@PathVariable Long userId) {
        var r=service.follow(current.id(),userId); return ResponseEntity.status(r.created()?201:200).body(r.result());
    }
    @DeleteMapping("/api/follows/{userId}") public ResponseEntity<Void> unfollow(@PathVariable Long userId) { service.unfollow(current.id(),userId); return ResponseEntity.noContent().build(); }
    @PostMapping("/api/follow-requests/{followId}/accept") public FollowResult accept(@PathVariable Long followId) { return service.respond(current.id(),followId,true); }
    @PostMapping("/api/follow-requests/{followId}/reject") public FollowResult reject(@PathVariable Long followId) { return service.respond(current.id(),followId,false); }
    @GetMapping("/api/users/{userId}/followers") public PageResponse<Summary> followers(@PathVariable Long userId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(current.id(),userId,true,page,size); }
    @GetMapping("/api/users/{userId}/following") public PageResponse<Summary> following(@PathVariable Long userId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(current.id(),userId,false,page,size); }
    @GetMapping("/api/users/me/follow-requests") public PageResponse<FollowRequest> requests(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.requests(current.id(),page,size); }
    @PostMapping("/api/users/{userId}/blocks") public ResponseEntity<Void> block(@PathVariable Long userId) { service.block(current.id(),userId); return ResponseEntity.noContent().build(); }
    @DeleteMapping("/api/users/{userId}/blocks") public ResponseEntity<Void> unblock(@PathVariable Long userId) { service.unblock(current.id(),userId); return ResponseEntity.noContent().build(); }
    @GetMapping("/api/users/me/blocks") public PageResponse<RelationshipService.BlockItem> blocks(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.blockList(current.id(),page,size); }
}
