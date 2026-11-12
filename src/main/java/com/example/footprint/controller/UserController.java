package com.example.footprint.controller;

import com.example.footprint.dto.CommonDtos.PageResponse;
import com.example.footprint.dto.GuestbookBoundResponseDto;
import com.example.footprint.security.CurrentUserProvider;
import com.example.footprint.service.ProfileImageService;
import com.example.footprint.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import static com.example.footprint.dto.UserDtos.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService users;
    private final ProfileImageService images;
    private final CurrentUserProvider current;

    @GetMapping("/me")
    public Profile me() {
        return users.profile(current.id(), current.id());
    }

    @GetMapping("/{userId}")
    public Profile get(@PathVariable Long userId) {
        return users.profile(current.id(), userId);
    }

    @PatchMapping("/me")
    public Profile patch(@Valid @RequestBody Patch request) {
        return users.patch(current.id(), request);
    }

    @GetMapping("/search")
    public PageResponse<Summary> search(
            @RequestParam String q,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return users.search(current.id(), q, type, page, size);
    }

    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Image upload(@RequestParam("file") MultipartFile file) {
        return images.replace(current.id(), file);
    }

    @DeleteMapping("/me/profile-image")
    public Image deleteImage() {
        return images.delete(current.id());
    }

    @GetMapping("/me/guestbooks")
    public List<GuestbookBoundResponseDto> guestbooks() {
        return users.getMyGuestbooks(current.id()).stream()
                .map(GuestbookBoundResponseDto::fromEntity).toList();
    }
}
