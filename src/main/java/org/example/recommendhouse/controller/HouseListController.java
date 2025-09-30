package org.example.recommendhouse.controller;

import lombok.RequiredArgsConstructor;
import org.example.recommendhouse.dto.HouseListResponse;
import org.example.recommendhouse.service.HouseListService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/houses")
@RequiredArgsConstructor
public class HouseListController {

    private final HouseListService houseListService;

    @GetMapping("/list")
    public ResponseEntity<Page<HouseListResponse>> getHouseList(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "houseType", required = false) String houseType,
            @RequestParam(value = "minArea", required = false) Double minArea,
            @RequestParam(value = "maxArea", required = false) Double maxArea,
            @RequestParam(value = "minDeposit", required = false) Long minDeposit,
            @RequestParam(value = "maxDeposit", required = false) Long maxDeposit,
            @RequestParam(value = "minMonthly", required = false) Long minMonthly,
            @RequestParam(value = "maxMonthly", required = false) Long maxMonthly,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Pageable pageable) {

        Page<HouseListResponse> houses = houseListService.getHouseList(
                keyword, city, houseType, minArea, maxArea,
                minDeposit, maxDeposit, minMonthly, maxMonthly, pageable);

        return ResponseEntity.ok(houses);
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> getCities() {
        List<String> cities = houseListService.getAllCities();
        return ResponseEntity.ok(cities);
    }

    @GetMapping("/house-types")
    public ResponseEntity<List<String>> getHouseTypes() {
        List<String> houseTypes = houseListService.getAllHouseTypes();
        return ResponseEntity.ok(houseTypes);
    }

    @GetMapping("/popular")
    public ResponseEntity<Page<HouseListResponse>> getPopularHouses(  // 이것도 Page로 수정
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "10") int size,
                                                                      Pageable pageable) {

        Page<HouseListResponse> popularHouses = houseListService.getPopularHouses(pageable);
        return ResponseEntity.ok(popularHouses);
    }

}

