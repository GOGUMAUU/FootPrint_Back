package com.example.footprint.service;

import com.example.footprint.dto.UserDtos;
import com.example.footprint.exception.ApiException;
import com.example.footprint.repository.UserRepository;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileImageService {
    private final UserRepository users;
    private final Path root;
    public ProfileImageService(UserRepository users, @Value("${file.upload-dir:uploads}") String uploadDir) {
        this.users = users;
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("profiles");
    }
    private String store(MultipartFile file) {
        if(file==null || file.isEmpty()) throw ApiException.bad("EMPTY_IMAGE","사진 파일이 비어 있습니다.");
        if(file.getSize()>5L*1024*1024) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,"FILE_TOO_LARGE","프로필 사진은 5MiB 이하만 가능합니다.");
        if(!Set.of("image/jpeg","image/png","image/webp").contains(Objects.toString(file.getContentType(),"")))
            throw ApiException.bad("INVALID_IMAGE","JPEG, PNG, WebP만 가능합니다.");
        Path destination=null;
        boolean decodedImage=false;
        try(InputStream source=file.getInputStream(); ImageInputStream in=ImageIO.createImageInputStream(source)) {
            var readers=ImageIO.getImageReaders(in);
            if(!readers.hasNext()) throw ApiException.bad("INVALID_IMAGE","이미지 내용이 올바르지 않습니다.");
            ImageReader reader=readers.next();
            BufferedImage decoded;
            try {
                reader.setInput(in,true,true);
                String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                if(!Set.of("jpeg","jpg","png","webp").contains(format)) throw ApiException.bad("INVALID_IMAGE","지원하지 않는 이미지 형식입니다.");
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width<=0 || height<=0 || (long)width*height>32_000_000) throw ApiException.bad("IMAGE_DIMENSIONS","이미지 픽셀 수가 너무 큽니다.");
                decoded=reader.read(0);
            } finally { reader.dispose(); }
            decodedImage=true;
            double scale=Math.min(1d,512d/Math.max(decoded.getWidth(),decoded.getHeight()));
            BufferedImage resized=new BufferedImage(Math.max(1,(int)Math.round(decoded.getWidth()*scale)),Math.max(1,(int)Math.round(decoded.getHeight()*scale)),BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics=resized.createGraphics();
            try { graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC); graphics.drawImage(decoded,0,0,resized.getWidth(),resized.getHeight(),null); }
            finally { graphics.dispose(); decoded.flush(); }
            Files.createDirectories(root);
            String name=UUID.randomUUID()+".webp"; destination=root.resolve(name);
            // A new BufferedImage and writer omit original EXIF and other metadata.
            if(!ImageIO.write(resized,"webp",destination.toFile())) throw new IOException("WebP encoder unavailable");
            resized.flush();
            return "/files/profiles/"+name;
        } catch(ApiException e) { throw e; }
        catch(IOException | RuntimeException e) {
            if(destination!=null) try { Files.deleteIfExists(destination); } catch(IOException ignored) {}
            throw new ApiException(decodedImage?HttpStatus.INTERNAL_SERVER_ERROR:HttpStatus.BAD_REQUEST,
                    decodedImage?"IMAGE_STORAGE_FAILED":"INVALID_IMAGE","이미지를 읽거나 저장하지 못했습니다.");
        }
    }
    private void deleteStored(String url) {
        if(url==null || !url.matches("^/files/profiles/[0-9a-f-]{36}\\.webp$")) return;
        try { Files.deleteIfExists(root.resolve(url.substring("/files/profiles/".length()))); }
        catch(IOException e) { LoggerFactory.getLogger(getClass()).warn("Profile image cleanup failed: {}",url); }
    }

    @Transactional
    public UserDtos.Image replace(Long userId,MultipartFile file) {
        var user=users.lockActive(userId).orElseThrow(ApiException::missing);
        String old=user.getProfileImageUrl();
        String fresh=store(file);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if(status==STATUS_COMMITTED) deleteStored(old); else deleteStored(fresh);
            }
        });
        user.setProfileImageUrl(fresh);
        users.flush();
        return new UserDtos.Image(fresh);
    }
    @Transactional
    public UserDtos.Image delete(Long userId) {
        var user=users.lockActive(userId).orElseThrow(ApiException::missing);
        String old=user.getProfileImageUrl(); user.setProfileImageUrl(null);
        deleteAfterCommit(old);
        return new UserDtos.Image(null);
    }
    public void deleteAfterCommit(String old) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { deleteStored(old); }
        });
    }
}
