package com.example.footprint.repository;

import com.example.footprint.entity.Session;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<Session, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Session s where s.sessionId = :id")
    Optional<Session> lock(@Param("id") String id);
    List<Session> findByUserId(Long userId);
}
