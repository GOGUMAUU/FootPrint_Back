package com.example.footprint.repository;

import com.example.footprint.entity.UserConsent;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ConsentRepository extends JpaRepository<UserConsent, Long> {
    List<UserConsent> findByUserIdAndTypeAndWithdrawnAtIsNull(Long userId, String type);
    boolean existsByUserIdAndTypeAndVersionAndWithdrawnAtIsNull(Long userId, String type, String version);
}
