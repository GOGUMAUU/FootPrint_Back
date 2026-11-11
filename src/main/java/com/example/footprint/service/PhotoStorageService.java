package com.example.footprint.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 사진 저장 (현재는 서버 폴더). 클라우드(S3 등)로 바꿀 때 이 클래스만 교체하면 됩니다.
 */
@Service
public class PhotoStorageService {

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Map<String, String> EXT_BY_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");
    private static final Pattern STORED_PATH = Pattern.compile("^/files/[0-9a-f\\-]{36}\\.(jpg|png|webp)$");

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /** 파일을 저장하고 "/files/uuid.jpg" 형태의 경로를 반환합니다. */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진 파일이 비어 있습니다.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진은 10MB 이하만 올릴 수 있습니다.");
        }
        String ext = EXT_BY_TYPE.get(file.getContentType());
        if (ext == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "jpg, png, webp 형식만 올릴 수 있습니다.");
        }
        String name = UUID.randomUUID() + "." + ext;
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(name));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "사진 저장에 실패했습니다.");
        }
        return "/files/" + name;
    }

    /** 이 서버가 발급한 경로인지, 실제 파일이 있는지 확인합니다. */
    public boolean exists(String path) {
        return path != null && STORED_PATH.matcher(path).matches() && Files.exists(resolve(path));
    }

    public void delete(String path) {
        if (path == null || !STORED_PATH.matcher(path).matches()) return;
        try {
            Files.deleteIfExists(resolve(path));
        } catch (IOException ignored) {
            // 파일 삭제 실패는 DB 처리에 영향을 주지 않음
        }
    }

    private Path resolve(String path) {
        return Paths.get(uploadDir).toAbsolutePath().normalize().resolve(path.substring("/files/".length()));
    }
}
