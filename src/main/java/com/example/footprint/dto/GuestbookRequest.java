package com.example.footprint.dto;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 작성(POST)과 수정(PUT)에 같이 사용합니다.
 * 수정 시에는 content, locationName, visibility, photoPaths만 사용하며 null인 항목은 변경하지 않습니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class GuestbookRequest {
    private Double latitude;          // 방명록 위치 (작성 시 필수)
    private Double longitude;
    private Double currentLatitude;   // 작성 시점의 내 현재 위치 (작성 시 필수)
    private Double currentLongitude;
    private String content;
    private String locationName;
    private String visibility;        // PUBLIC | PRIVATE | FOLLOWERS_ONLY (기본 PUBLIC)
    private List<String> photoPaths;  // /api/guestbook-photos 업로드 응답의 path 목록
}