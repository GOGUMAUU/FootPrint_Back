package com.example.footprint.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity @Table(name = "sessions") @Getter @Setter @NoArgsConstructor
public class Session {
    @Id @Column(length = 36) private String sessionId;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant expiresAt;
    private Instant lastUsedAt;
    private Instant revokedAt;
}
