package com.example.footprint.repository;

import com.example.footprint.entity.Quest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestRepository extends JpaRepository<Quest, Long> {
}
  