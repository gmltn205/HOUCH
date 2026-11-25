package org.example.recommendhouse.controller;

import org.example.recommendhouse.entity.HouseInfo;
import org.example.recommendhouse.service.HouseInfoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {
    private final HouseInfoService houseInfoService;

    public HomeController(HouseInfoService houseInfoService) {
        this.houseInfoService = houseInfoService;
    }

    @GetMapping("/")
    public String showHomePage(Model model) {
        List<HouseInfo> topHouses = houseInfoService.getTopViewedHouses();
        model.addAttribute("topHouses", topHouses);
        return "home";
    }
}
