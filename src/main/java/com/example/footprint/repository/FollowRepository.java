package com.example.footprint.repository;

import com.example.footprint.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    @Query("SELECT f.following.userId FROM Follow f WHERE f.follower.userId = :currentUserId AND f.status = 'ACCEPTED'")
    List<Long> findAcceptedFollowingIds(@Param("currentUserId") Long currentUserId);
    // 팔로우 승인(ACCEPTED) 상태 여부 확인
        boolean existsByFollower_UserIdAndFollowing_UserIdAndStatus(Long followerId, Long followingId, String status);
}