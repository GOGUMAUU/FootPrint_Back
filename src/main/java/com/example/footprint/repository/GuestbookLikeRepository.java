package com.example.footprint.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.footprint.entity.GuestbookLike;

public interface GuestbookLikeRepository extends JpaRepository<GuestbookLike, Long> {
    long countByGuestbook_GuestbookId(Long guestbookId);

    boolean existsByGuestbook_GuestbookIdAndUser_UserId(Long guestbookId, Long userId);

    void deleteByGuestbook_GuestbookIdAndUser_UserId(Long guestbookId, Long userId);

    void deleteByGuestbook_GuestbookId(Long guestbookId);
}
