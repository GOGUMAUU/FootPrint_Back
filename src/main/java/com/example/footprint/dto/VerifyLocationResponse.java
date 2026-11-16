package com.example.footprint.dto;

public record VerifyLocationResponse(
        boolean verified,
        double distanceMeters,
        int radiusMeters
) {
}
