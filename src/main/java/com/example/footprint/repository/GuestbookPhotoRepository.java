package com.example.footprint.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.footprint.entity.GuestbookPhoto;

public interface GuestbookPhotoRepository extends JpaRepository<GuestbookPhoto, Long> {
    List<GuestbookPhoto> findByFilePath(String filePath);
    List<GuestbookPhoto> findByGuestbook_GuestbookIdOrderBySortOrderAsc(Long guestbookId);
}
