package com.example.footprint.repository;

import com.example.footprint.entity.PolicyDocument;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface PolicyRepository extends JpaRepository<PolicyDocument, Long> {
    List<PolicyDocument> findByPublishedTrueOrderByPublishedAtDescPolicyIdDesc();
    Optional<PolicyDocument> findFirstByTypeAndPublishedTrueOrderByPublishedAtDescPolicyIdDesc(String type);
}
