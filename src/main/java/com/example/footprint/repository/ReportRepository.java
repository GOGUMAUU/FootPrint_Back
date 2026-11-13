package com.example.footprint.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.footprint.entity.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {
    boolean existsByGuestbook_GuestbookIdAndReporter_UserId(Long guestbookId, Long reporterId);

    void deleteByGuestbook_GuestbookId(Long guestbookId);
}
