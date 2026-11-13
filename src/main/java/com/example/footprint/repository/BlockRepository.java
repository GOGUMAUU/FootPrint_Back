package com.example.footprint.repository;
import com.example.footprint.entity.Block;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
public interface BlockRepository extends JpaRepository<Block, Long> {
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    @Query("select count(b) > 0 from Block b where (b.blockerId = :a and b.blockedId = :b) or (b.blockerId = :b and b.blockedId = :a)")
    boolean between(Long a, Long b);
    Slice<Block> findByBlockerIdOrderByCreatedAtDescBlockIdDesc(Long blockerId, Pageable page);
    List<Block> findByBlockerIdOrBlockedId(Long blockerId, Long blockedId);
}
