package org.example.recommendhouse.controller;

import org.example.recommendhouse.entity.RentalHousing;
import org.example.recommendhouse.service.ApplyhomeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/applyhome")
public class ApplyhomeController {

    private final ApplyhomeService applyhomeService;

    public ApplyhomeController(ApplyhomeService applyhomeService) {
        this.applyhomeService = applyhomeService;
    }

    @GetMapping("/gyeonggi-rentals")
    public ResponseEntity<List<RentalHousing>> getGyeonggiRentals() {
        List<RentalHousing> rentals = applyhomeService.getGyeonggiPublicRentals();
        return ResponseEntity.ok(rentals);
    }
}
