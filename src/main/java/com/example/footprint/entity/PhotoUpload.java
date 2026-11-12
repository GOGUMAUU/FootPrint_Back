package com.example.footprint.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="photo_uploads") @Getter @Setter @NoArgsConstructor
public class PhotoUpload {
    @Id @Column(length=255) private String path;
    @Column(nullable=false) private Long userId;
    @Column(nullable=false) private Instant createdAt;
}
