package com.example.footprint.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity @Table(name="user_contents") @Getter @Setter @NoArgsConstructor
public class UserConsent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long consentId;
    @Column(nullable=false) private Long userId;
    @Column(nullable=false, length=30) private String type;
    @Column(nullable=false, length=40) private String version;
    @Column(nullable=false) private Instant consentedAt;
    private Instant withdrawnAt;
}
