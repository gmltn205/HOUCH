package org.example.recommendhouse.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.recommendhouse.entity.Favorite;
import org.example.recommendhouse.entity.User;
import org.example.recommendhouse.service.FavoriteService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;

    // 찜 토글 API
    @PostMapping("/api/favorites/house/{houseInfoId}")
    public ResponseEntity<Boolean> toggleFavorite(
            @PathVariable(name = "houseInfoId") Long houseInfoId,  //
            HttpSession session) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(false);
        }

        boolean isFavorited = favoriteService.toggleFavorite(user.getUserId(), houseInfoId);
        return ResponseEntity.ok(isFavorited);
    }

    // 찜 목록 전체 페이지 조회 (마이페이지)
    @GetMapping("/favorites")
    public ModelAndView getAllFavorites(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }

        Page<Favorite> favorites = favoriteService.getAllFavorites(user, page, size);
        
        ModelAndView modelAndView = new ModelAndView("favorites");
        modelAndView.addObject("favorites", favorites);
        modelAndView.addObject("currentPage", page);
        modelAndView.addObject("totalPages", favorites.getTotalPages());
        
        return modelAndView;
    }
}