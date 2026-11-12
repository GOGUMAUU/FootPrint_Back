package com.example.footprint.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import com.example.footprint.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailAndDeletedAtIsNull(String email);
    Optional<User> findByUserIdAndDeletedAtIsNull(Long userId);
    boolean existsByEmail(String email);
    boolean existsByFriendCode(String friendCode);
    boolean existsByUserIdAndDeletedAtIsNull(Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.userId = :id and u.deletedAt is null")
    Optional<User> lockActive(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.userId = :id")
    Optional<User> lockAny(Long id);
    @Query("select u from User u where u.deletedAt is null and u.userId <> :viewer " +
           "and ((:nickname = true and lower(u.nickname) like :pattern escape '!') or (:code = true and u.friendCode = :friendCode)) " +
           "and not exists (select b from Block b where (b.blockerId = :viewer and b.blockedId = u.userId) or (b.blockerId = u.userId and b.blockedId = :viewer)) " +
           "order by u.nickname asc, u.userId asc")
    Slice<User> search(Long viewer, boolean nickname, boolean code, String pattern, String friendCode, Pageable page);
}
