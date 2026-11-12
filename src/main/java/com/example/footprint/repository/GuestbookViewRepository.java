package com.example.footprint.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.footprint.entity.GuestbookView;

public interface GuestbookViewRepository extends JpaRepository<GuestbookView, Long> {
    boolean existsByGuestbook_GuestbookIdAndUser_UserId(Long guestbookId, Long userId);

    long countByGuestbook_GuestbookId(Long guestbookId);

    void deleteByGuestbook_GuestbookId(Long guestbookId);
}
