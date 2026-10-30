package com.example.footprint.repository.maps;

import com.example.footprint.entity.Guestbook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface GuestbookRepository extends JpaRepository<Guestbook, Long> {

    // 1. 특정 유저(내 유저 ID)가 작성한 방명록 조회
    List<Guestbook> findByUser_UserId(Long userId);

    // 2. 영역 내 방명록 조회 (지도 영역 바운딩 박스)
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

    // 3. 반경 내 주변 방명록 조회
    @Query(value = "SELECT g.*, " +
            "(6371 * acos(cos(radians(:lat)) * cos(radians(g.latitude)) * " +
            "cos(radians(g.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(g.latitude)))) AS distance " +
            "FROM guestbook g " +
            "HAVING distance <= :radiusKm " +
            "ORDER BY distance ASC", nativeQuery = true)
    List<Guestbook> findNearbyGuestbooks(@Param("lat") double lat, 
                                         @Param("lng") double lng, 
                                         @Param("radiusKm") double radiusKm);
}