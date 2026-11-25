package org.example.recommendhouse.controller;


import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.recommendhouse.entity.HouseInfo;
import org.example.recommendhouse.entity.User;
import org.example.recommendhouse.service.FavoriteService;
import org.example.recommendhouse.service.HouseInfoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class HouseInfoViewController {
    private final HouseInfoService houseInfoService;
    private final FavoriteService favoriteService;

    @GetMapping("/houses/{id}")
    public String showHouseDetail(
            @PathVariable("id") Long id,
            Model model,
            HttpSession session) {

        HouseInfo houseInfo = houseInfoService.getHouseById(id);
        model.addAttribute("house", houseInfo);

        // 로그인한 사용자의 찜 상태 확인
        User user = (User) session.getAttribute("user");
        if (user != null) {
            boolean isFavorite = favoriteService.isFavorite(user, id);
            model.addAttribute("isFavorite", isFavorite);
        }

        return "house-detail";  // house-detail.html을 렌더링
    }
}