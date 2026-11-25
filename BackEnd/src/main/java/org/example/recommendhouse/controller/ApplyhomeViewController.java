package org.example.recommendhouse.controller;

import org.example.recommendhouse.entity.RentalHousing;
import org.example.recommendhouse.service.ApplyhomeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/applyhome")
public class ApplyhomeViewController {

    private final ApplyhomeService applyhomeService;

    public ApplyhomeViewController(ApplyhomeService applyhomeService) {
        this.applyhomeService = applyhomeService;
    }

    @GetMapping("/gyeonggi-rentals")
    public String getGyeonggiRentals(Model model) {
        List<RentalHousing> rentals = applyhomeService.getGyeonggiPublicRentals();
        model.addAttribute("rentals", rentals);
        return "rental-list";
    }
}
