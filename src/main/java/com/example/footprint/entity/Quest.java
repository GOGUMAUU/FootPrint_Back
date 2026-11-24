package com.example.footprint.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "quests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quest_id")
    private Long questId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "region_name", nullable = false, length = 50)
    private String regionName;

    @Column(name = "target_latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal targetLatitude;

    @Column(name = "target_longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal targetLongitude;

    @Column(name = "target_radius", nullable = false)
    private Integer targetRadius;

    @Column(name = "badge_image_url", length = 255)
    private String badgeImageUrl;

    // 도전과제 생성자 (null이면 기본 도전과제)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    // 도전과제 유형 (기본값: 일반 도전)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "quest_type", nullable = false, length = 20)
    private QuestType questType = QuestType.SINGLE;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}