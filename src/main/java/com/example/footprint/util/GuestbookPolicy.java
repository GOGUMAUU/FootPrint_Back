package com.example.footprint.util;

/** 방명록 정책 값. 프론트 AppConfig와 맞춰서 사용합니다. */
public final class GuestbookPolicy {
    private GuestbookPolicy() {}

    public static final double CREATE_ALLOWED_RADIUS_METERS = 150.0;
    public static final double UNLOCK_RADIUS_METERS = 100.0;
    public static final int CONTENT_MAX = 120;
    public static final int MAX_PHOTOS = 3;

    // 국내 좌표 범위
    public static final double LAT_MIN = 33.0, LAT_MAX = 38.7;
    public static final double LNG_MIN = 124.5, LNG_MAX = 131.9;
}