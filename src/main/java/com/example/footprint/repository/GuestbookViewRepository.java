package com.example.footprint.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.footprint.entity.GuestbookView;

public interface GuestbookViewRepository extends JpaRepository<GuestbookView, Long> {
    boolean existsByGuestbook_GuestbookIdAndUser_UserId(Long guestbookId, Long userId);

    long countByGuestbook_GuestbookId(Long guestbookId);

    void deleteByGuestbook_GuestbookId(Long guestbookId);

    // ---------- 👣 다녀간 발자국: 그 자리에 와서 열어 본 사람 수 (작성자 제외, 같은 사람 1번) ----------
    @org.springframework.data.jpa.repository.Query(
            "SELECT COUNT(DISTINCT v.user.userId) FROM GuestbookView v " +
            "WHERE v.guestbook.guestbookId = :id AND v.user.userId <> v.guestbook.user.userId " +
            "AND (:since IS NULL OR v.viewedAt >= :since)")
    long countFootprints(@org.springframework.data.repository.query.Param("id") Long guestbookId,
                         @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

    @org.springframework.data.jpa.repository.Query(
            "SELECT COUNT(DISTINCT v.user.userId) FROM GuestbookView v " +
            "WHERE v.guestbook.guestbookId IN :ids AND v.user.userId <> v.guestbook.user.userId " +
            "AND (:since IS NULL OR v.viewedAt >= :since)")
    long countFootprintsIn(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> guestbookIds,
                           @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);
}
