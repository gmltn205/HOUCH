package org.example.recommendhouse.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.recommendhouse.entity.Favorite;
import org.example.recommendhouse.entity.RentalHousing;
import org.example.recommendhouse.entity.User;
import org.example.recommendhouse.service.FavoriteService;
import org.example.recommendhouse.service.favorite_cheongyak;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
@Slf4j
@Controller
@RequiredArgsConstructor
public class MyPageController {
    private final FavoriteService favoriteService;
    private final favorite_cheongyak favorite_cheongyak;


    @GetMapping("/mypage")
    public String showMyPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        log.info("현재 세션의 사용자: {}", user);
        // 상위 3개 찜 목록 조회
        List<Favorite> topFavorites = favoriteService.getTopFavorites(user);

        // 찜 목록 총 개수
        long favoriteCount = favoriteService.getFavoriteCount(user);

        List<RentalHousing> topFavorites_c = favorite_cheongyak.getTopFavoriteRentals(user);

        // 찜 목록 총 개수
        long favoriteCount_c = favorite_cheongyak.getFavoriteRentalCount(user);

        model.addAttribute("topFavorites", topFavorites);
        model.addAttribute("favoriteCount", favoriteCount);
        model.addAttribute("topFavoriteRentals", topFavorites_c);
        model.addAttribute("favoriteRentalCount", favoriteCount_c);
        log.info("Model에 추가된 찜 목록: {}", topFavorites);
        log.info("Model에 추가된 청약 찜 목록: {}", topFavorites_c);

        return "mypage";
    }

    @GetMapping("/mypage/applications")
    public String showApplicationStatus(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<RentalHousing> applications = favorite_cheongyak.getTopFavoriteRentals(user);
        model.addAttribute("applications", applications);

        return "application-status";  // application-status.html을 새로 만들 것입니다
    }
}