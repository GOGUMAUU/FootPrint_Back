package com.example.footprint.repository;

import com.example.footprint.entity.Guestbook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.math.BigDecimal;

public interface GuestbookRepository extends JpaRepository<Guestbook, Long> {
    // 특정 유저(내 유저 ID)가 작성한 방명록 조회
        List<Guestbook> findByUser_UserId(Long userId);
    // 영역 내 방명록 조회 (지도 영역 바운딩 박스)
    @Query("SELECT g FROM Guestbook g " +
           "WHERE g.user.userId IN :userIds " +
           "AND g.latitude BETWEEN :southLat AND :northLat " +
           "AND g.longitude BETWEEN :westLng AND :eastLng")
    List<Guestbook> findInBound(
            @Param("userIds") List<Long> userIds,
            @Param("southLat") BigDecimal southLat,
            @Param("northLat") BigDecimal northLat,
            @Param("westLng") BigDecimal westLng,
            @Param("eastLng") BigDecimal eastLng
    );
}