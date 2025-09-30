package org.example.recommendhouse.controller;

import lombok.RequiredArgsConstructor;
import org.example.recommendhouse.entity.HouseInfo;

import org.example.recommendhouse.service.HouseInfoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/houses")
@RequiredArgsConstructor
public class HouseInfoController {
    private final HouseInfoService houseInfoService;

    @GetMapping("/import")
    public ResponseEntity<String> importData() {
        houseInfoService.importCsvData();
        return ResponseEntity.ok("데이터 임포트 완료");
    }

    @GetMapping("/search")
    public ResponseEntity<List<HouseInfo>> searchHouses(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String houseType,
            @RequestParam(required = false) Double minArea,
            @RequestParam(required = false) Double maxArea) {

        List<HouseInfo> houses = houseInfoService.searchHouses(city, houseType, minArea, maxArea);
        return ResponseEntity.ok(houses);
    }

    @GetMapping("/all")
    public ResponseEntity<List<HouseInfo>> getAllHouses() {
        List<HouseInfo> houses = houseInfoService.getAllHouses();
        return ResponseEntity.ok(houses);
    }

    @GetMapping("/api/houses/popular")
    public ResponseEntity<Page<HouseInfo>> getPopularHouses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<HouseInfo> houses = houseInfoService.getHousesByViewCount(pageable);
        return ResponseEntity.ok(houses);
    }
}