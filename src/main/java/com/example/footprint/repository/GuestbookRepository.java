package com.example.footprint.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.footprint.entity.Guestbook;

@Repository
public interface GuestbookRepository extends JpaRepository<Guestbook, Long> {

    // 1. 특정 유저(내 유저 ID)가 작성한 방명록 조회
    List<Guestbook> findByUser_UserId(Long userId);

    // 2. 영역 내 방명록 조회 (팔로잉/내 지도)
    //    다른 사람의 PRIVATE 글은 제외, 내 글은 전부
    @Query("SELECT g FROM Guestbook g " +
           "WHERE g.user.userId IN :userIds " +
           "AND (g.user.userId = :currentUserId OR g.visibility <> :privateVisibility) " +
           "AND g.latitude BETWEEN :southLat AND :northLat " +
           "AND g.longitude BETWEEN :westLng AND :eastLng")
    List<Guestbook> findInBound(
            @Param("userIds") List<Long> userIds,
            @Param("currentUserId") Long currentUserId,
            @Param("privateVisibility") Guestbook.Visibility privateVisibility,
            @Param("southLat") BigDecimal southLat,
            @Param("northLat") BigDecimal northLat,
            @Param("westLng") BigDecimal westLng,
            @Param("eastLng") BigDecimal eastLng
    );

    // 3. 주변 방명록 후보 조회 (내 위치 기준 사각형 → 정확한 원형 거리는 서비스에서 계산)
    //    공개 글 + 내 글만
    @Query("SELECT g FROM Guestbook g " +
           "WHERE (g.visibility = :publicVisibility OR g.user.userId = :currentUserId) " +
           "AND g.latitude BETWEEN :southLat AND :northLat " +
           "AND g.longitude BETWEEN :westLng AND :eastLng")
    List<Guestbook> findVisibleInBox(
            @Param("currentUserId") Long currentUserId,
            @Param("publicVisibility") Guestbook.Visibility publicVisibility,
            @Param("southLat") BigDecimal southLat,
            @Param("northLat") BigDecimal northLat,
            @Param("westLng") BigDecimal westLng,
            @Param("eastLng") BigDecimal eastLng
    );

    // 4. 방명록별 좋아요 수 (한 번에 묶어서 조회) → [guestbookId, count]
    @Query("SELECT l.guestbook.guestbookId, COUNT(l) FROM GuestbookLike l " +
           "WHERE l.guestbook.guestbookId IN :ids " +
           "GROUP BY l.guestbook.guestbookId")
    List<Object[]> countLikesByGuestbookIds(@Param("ids") List<Long> ids);

    @Query("SELECT g FROM Guestbook g " +
           "WHERE g.user.userId = :targetUserId " +
           "AND g.user.deletedAt IS NULL " +
           "AND (:isOwner = true OR g.visibility <> :privateVis) " +
           "AND (:cutoff IS NULL OR g.createdAt >= :cutoff) " +
           "ORDER BY g.createdAt DESC")
    Page<Guestbook> findUserGuestbooksOrderByLatest(
            @Param("targetUserId") Long targetUserId,
            @Param("isOwner") boolean isOwner,
            @Param("privateVis") Guestbook.Visibility privateVis,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable);

    @Query("SELECT g FROM Guestbook g " +
           "WHERE g.user.userId = :targetUserId " +
           "AND g.user.deletedAt IS NULL " +
           "AND (:isOwner = true OR g.visibility <> :privateVis) " +
           "AND (:cutoff IS NULL OR g.createdAt >= :cutoff) " +
           "ORDER BY (SELECT COUNT(l) FROM GuestbookLike l WHERE l.guestbook = g) DESC, g.createdAt DESC")
    Page<Guestbook> findUserGuestbooksOrderByPopular(
            @Param("targetUserId") Long targetUserId,
            @Param("isOwner") boolean isOwner,
            @Param("privateVis") Guestbook.Visibility privateVis,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable);
}