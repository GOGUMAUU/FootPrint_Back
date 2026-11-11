package com.example.footprint.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.footprint.service.PhotoStorageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/guestbook-photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoStorageService storage;

    /** 사진 1장 업로드 → { "path": "/files/xxx.jpg", "url": "/files/xxx.jpg" } */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "purpose", required = false) String purpose) {
        String path = storage.store(file);
        return Map.of("path", path, "url", path);
    }
}