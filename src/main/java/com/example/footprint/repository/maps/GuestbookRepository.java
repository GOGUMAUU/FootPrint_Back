package com.example.footprint.repository.maps;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.footprint.entity.Guestbook;

@Repository
public interface GuestbookRepository extends JpaRepository<Guestbook, Long> {

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