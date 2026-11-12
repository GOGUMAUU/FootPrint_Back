package com.example.footprint.controller;

import com.example.footprint.security.CurrentUserProvider;
import com.example.footprint.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static com.example.footprint.dto.PolicyDtos.*;

@RestController @RequiredArgsConstructor
public class PolicyController {
    private final PolicyService service;
    private final CurrentUserProvider current;
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @GetMapping("/api/policies") public Documents list() { return new Documents(service.current().stream().map(Document::of).toList()); }
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @GetMapping("/api/policies/{type}") public Document get(@PathVariable String type) { return Document.of(service.policy(type)); }
    @PostMapping("/api/users/me/consents") public ResponseEntity<Void> consent(@Valid @RequestBody ConsentRequest request) {
        service.record(current.id(), request.consents()); return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/api/users/me/consents/{type}") public ResponseEntity<Void> withdraw(@PathVariable String type) {
        service.withdraw(current.id(), type); return ResponseEntity.noContent().build();
    }
}
