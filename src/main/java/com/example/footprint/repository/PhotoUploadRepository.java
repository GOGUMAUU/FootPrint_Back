package com.example.footprint.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.footprint.entity.PhotoUpload;
public interface PhotoUploadRepository extends JpaRepository<PhotoUpload,String> {}
