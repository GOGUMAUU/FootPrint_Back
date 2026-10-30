package com.example.footprint.service;

import com.example.footprint.dto.maps.NearbyGuestbookRequestDto;
import com.example.footprint.dto.maps.NearbyGuestbookResponseDto;
import com.example.footprint.entity.Guestbook;
import com.example.footprint.repository.maps.GuestbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

  /* @Service
@RequiredArgsConstructor
public class MapService {

    private final GuestbookRepository guestbookRepository;

    @Transactional(readOnly = true)
    public NearbyGuestbookResponseDto getNearbyGuestbooks(NearbyGuestbookRequestDto request, Long currentUserId) {
        double centerLat = request.getLatitude();
        double centerLng = request.getLongitude();
        int zoomLevel = request.getZoomLevel() != null ? request.getZoomLevel() : 15;

        double radiusKm = calculateRadiusByZoom(zoomLevel);
        List<Guestbook> rawGuestbooks = guestbookRepository.findNearbyGuestbooks(centerLat, centerLng, radiusKm);
         List<NearbyGuestbookResponseDto.GuestbookItem> items = processClusteringAndMapping(rawGuestbooks, centerLat, centerLng, zoomLevel);

        return NearbyGuestbookResponseDto.builder()
                .centerLatitude(centerLat)
                .centerLongitude(centerLng)
                .totalCount(items.size())
                .guestbooks(items)
                .build();
    }

    private double calculateRadiusByZoom(int zoom) {
        if (zoom <= 10) return 10.0;
        if (zoom <= 13) return 5.0;
        if (zoom <= 15) return 2.0;
        return 0.5;
        //줌레벨 테스트 필요함 
    }

    /* private List<NearbyGuestbookResponseDto.GuestbookItem> processClusteringAndMapping(
            List<Guestbook> list, double centerLat, double centerLng, int zoomLevel) {
        
        List<NearbyGuestbookResponseDto.GuestbookItem> result = new ArrayList<>();

        for (Guestbook g : list) {
            double distanceMeter = calculateDistanceMeter(centerLat, centerLng, g.getLatitude(), g.getLongitude());
            
            boolean isAccessible = distanceMeter <= 100.0;

            boolean isClustered = false; 
            int clusterCount = 1;

            result.add(NearbyGuestbookResponseDto.GuestbookItem.builder()
                    .guestbookId(g.getId())
                    .title(g.getTitle())
                    .thumbnailUrl(g.getThumbnailUrl())
                    .latitude(g.getLatitude())
                    .longitude(g.getLongitude())
                    .locationName(g.getLocationName())
                    .distanceMeter(Math.round(distanceMeter * 10.0) / 10.0)
                    .isAccessible(isAccessible)
                    .likeCount(g.getLikeCount())
                    .isClustered(isClustered)
                    .clusterCount(clusterCount)
                    .build());
        }

        return result;
    }

    private double calculateDistanceMeter(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; 
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; 
}}
*/ 
