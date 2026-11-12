package com.example.footprint.repository;

import com.example.footprint.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollower_UserIdAndFollowing_UserId(Long followerId, Long followingId);
    List<Follow> findByFollower_UserIdOrFollowing_UserId(Long followerId, Long followingId);
    @Query("select f from Follow f where f.following.userId = :id and f.status = 'ACCEPTED' and f.follower.deletedAt is null " +
           "and not exists (select b from Block b where (b.blockerId = :viewer and b.blockedId = f.follower.userId) or (b.blockedId = :viewer and b.blockerId = f.follower.userId)) order by f.createdAt desc, f.followId desc")
    Slice<Follow> followers(Long id, Long viewer, Pageable page);
    @Query("select f from Follow f where f.follower.userId = :id and f.status = 'ACCEPTED' and f.following.deletedAt is null " +
           "and not exists (select b from Block b where (b.blockerId = :viewer and b.blockedId = f.following.userId) or (b.blockedId = :viewer and b.blockerId = f.following.userId)) order by f.createdAt desc, f.followId desc")
    Slice<Follow> following(Long id, Long viewer, Pageable page);
    Slice<Follow> findByFollowing_UserIdAndStatusOrderByCreatedAtDescFollowIdDesc(Long userId, String status, Pageable page);

    @Query("SELECT f.following.userId FROM Follow f WHERE f.follower.userId = :currentUserId AND f.status = 'ACCEPTED'")
    List<Long> findAcceptedFollowingIds(@Param("currentUserId") Long currentUserId);
    // 팔로우 승인(ACCEPTED) 상태 여부 확인
        boolean existsByFollower_UserIdAndFollowing_UserIdAndStatus(Long followerId, Long followingId, String status);
}
