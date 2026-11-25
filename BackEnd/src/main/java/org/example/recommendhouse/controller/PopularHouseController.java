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

@RestController
@RequestMapping("/houses")
@RequiredArgsConstructor
public class PopularHouseController {

    private final HouseInfoService houseInfoService;

    @GetMapping("/popular")
    public ResponseEntity<Page<HouseInfo>> getPopularHouses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String houseType,
            @RequestParam(required = false) Long minDeposit,
            @RequestParam(required = false) Long maxDeposit,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<HouseInfo> houses = houseInfoService.getFilteredHousesByViewCount(
                keyword, city, houseType, minDeposit, maxDeposit, pageable);
        return ResponseEntity.ok(houses);
    }
}