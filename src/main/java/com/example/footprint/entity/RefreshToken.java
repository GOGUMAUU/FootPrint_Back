package com.example.footprint.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity @Table(name = "refresh_tokens") @Getter @Setter @NoArgsConstructor
public class RefreshToken {
    @Id @Column(length = 64) private String tokenHash;
    @Column(nullable = false, length = 36) private String sessionId;
    private Instant usedAt;
}
