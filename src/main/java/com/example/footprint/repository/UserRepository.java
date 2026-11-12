package com.example.footprint.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.footprint.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}