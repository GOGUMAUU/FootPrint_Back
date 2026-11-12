package com.example.footprint.service;

import com.example.footprint.entity.PhotoUpload;
import com.example.footprint.exception.ApiException;
import com.example.footprint.repository.GuestbookPhotoRepository;
import com.example.footprint.repository.PhotoUploadRepository;
import com.example.footprint.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PhotoStorageService {
    private final PhotoUploadRepository uploads;
    private final GuestbookPhotoRepository photos;
    private final UserService access;
    private final UserRepository users;
    private final Clock clock;

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Map<String, String> EXT_BY_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");
    private static final Pattern STORED_PATH = Pattern.compile("^/files/[0-9a-f\\-]{36}\\.(jpg|png|webp)$");

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /** 파일을 저장하고 "/files/uuid.jpg" 형태의 경로를 반환합니다. */
    private String storeRaw(MultipartFile file) {
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

    public org.springframework.core.io.Resource resource(String path) {
        if (!exists(path)) throw ApiException.missing();
        return new org.springframework.core.io.FileSystemResource(resolve(path));
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

    @Transactional
    public String upload(Long userId, MultipartFile file) {
        users.lockActive(userId).orElseThrow(ApiException::missing);
        String path = storeRaw(file);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) delete(path);
            }
        });
        var upload = new PhotoUpload();
        upload.setPath(path);
        upload.setUserId(userId);
        upload.setCreatedAt(clock.instant());
        uploads.save(upload);
        return path;
    }

    public void requireOwned(Long userId, String path) {
        if (!exists(path) || !uploads.findById(path).map(upload -> upload.getUserId().equals(userId)).orElse(false)) {
            throw ApiException.bad("INVALID_PHOTO_OWNER", "본인이 업로드한 사진만 사용할 수 있습니다.");
        }
    }

    @Transactional(readOnly = true)
    public Resource read(Long viewer, String path) {
        if (!exists(path)) throw ApiException.missing();
        boolean owner = uploads.findById(path).map(upload -> upload.getUserId().equals(viewer)).orElse(false);
        if (!owner && photos.findByFilePath(path).stream()
                .noneMatch(photo -> access.canReadGuestbook(viewer, photo.getGuestbook()))) {
            throw ApiException.missing();
        }
        return resource(path);
    }
}
