package com.example.footprint.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/places")
public class PlaceReverseController {

    @Value("${kakao.api.key}")
    private String kakaoKey;

    private final RestClient client = RestClient.create();

    @GetMapping("/reverse")
    @SuppressWarnings("unchecked")
    public Map<String, Object> reverse(@RequestParam double lat, @RequestParam double lng) {
        Map<String, Object> out = new LinkedHashMap<>();
        String name = null, road = null, jibun = null;
        try {
            Map<String, Object> root = client.get()
                    .uri("https://dapi.kakao.com/v2/local/geo/coord2address.json?x={x}&y={y}", lng, lat)
                    .header("Authorization", "KakaoAK " + kakaoKey)
                    .retrieve()
                    .body(Map.class);
            List<Map<String, Object>> docs = root == null ? null : (List<Map<String, Object>>) root.get("documents");
            if (docs != null && !docs.isEmpty()) {
                Map<String, Object> d = docs.get(0);
                Map<String, Object> r = (Map<String, Object>) d.get("road_address");
                Map<String, Object> a = (Map<String, Object>) d.get("address");
                if (r != null) {
                    road = (String) r.get("address_name");
                    String b = (String) r.get("building_name");
                    if (b != null && !b.isBlank()) name = b;
                }
                if (a != null) jibun = (String) a.get("address_name");
            }
        } catch (Exception ignored) {
            // 조회 실패 시 모두 null로 내려가고, 앱은 위치 좌표만 쓰면 됨
        }
        String display = name != null ? name : (road != null ? road : jibun);
        out.put("name", name);
        out.put("roadAddress", road);
        out.put("address", jibun);
        out.put("display", display);
        return out;
    }
}
