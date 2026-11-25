package org.example.recommendhouse.controller;

import org.example.recommendhouse.entity.HouseInfo;
import org.example.recommendhouse.repository.HouseInfoRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/properties")
public class MapPropertyController {

    private final HouseInfoRepository houseInfoRepository;

    public MapPropertyController(HouseInfoRepository houseInfoRepository) {
        this.houseInfoRepository = houseInfoRepository;
    }

    /**
     * 특정 좌표(lat, lon)를 기준으로 주변 매물 조회
     */
    @GetMapping("/nearby")
    public List<HouseInfo> getNearbyProperties(@RequestParam double lat,
                                               @RequestParam double lon) {
        // 범위를 약 3km 정도로 확장
        double range = 0.03; // 약 3km 반경
        double latStart = lat - range;
        double latEnd   = lat + range;
        double lonStart = lon - range;
        double lonEnd   = lon + range;

        List<HouseInfo> properties = houseInfoRepository.findByLatitudeBetweenAndLongitudeBetween(
                latStart, latEnd, lonStart, lonEnd
        );

        // 디버깅을 위한 로그
        System.out.println("Found " + properties.size() + " properties nearby");
        return properties;
    }
}
