package com.example.footprint.controller;

import com.example.footprint.security.CurrentUserProvider;
import com.example.footprint.service.PhotoStorageService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class PhotoController {
    private final PhotoStorageService storage;
    private final CurrentUserProvider current;

    /** 사진 한 장 업로드 후 기존 path/url 응답을 유지한다. */
    @PostMapping(value = "/api/guestbook-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "purpose", required = false) String purpose) {
        String path = storage.upload(current.id(), file);
        return Map.of("path", path, "url", path);
    }

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> read(@PathVariable String filename) {
        Resource file = storage.read(current.id(), "/files/" + filename);
        String type = filename.endsWith(".jpg") ? "image/jpeg"
                : filename.endsWith(".png") ? "image/png" : "image/webp";
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(type)).body(file);
    }
}
