package org.example.recommendhouse.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.recommendhouse.entity.RentalHousing;
import org.example.recommendhouse.entity.User;

import org.example.recommendhouse.service.favorite_cheongyak;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class favorite_cheongyack_controller {
    private final favorite_cheongyak favoriteService;

    @PostMapping("/{houseManageNo}")
    public ResponseEntity<Boolean> toggleFavorite(
            @PathVariable(name = "houseManageNo") String houseManageNo, HttpSession session) {  // 여기 수정
        // SecurityContextHolder 대신 세션에서 사용자 정보 가져오기
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(false);
        }

        boolean isFavorited = favoriteService.toggleFavorite(houseManageNo,user);
        return ResponseEntity.ok(isFavorited);
    }

    @PostMapping("/rental/{houseManageNo}")
    public ResponseEntity<Boolean> toggleRentalFavorite(
            @PathVariable(name = "houseManageNo") String houseManageNo,HttpSession session) {  // 여기 수정

        // SecurityContextHolder 대신 세션에서 사용자 정보 가져오기
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(false);
        }
        boolean isFavorited = favoriteService.toggleRentalFavorite(houseManageNo, user);
        return ResponseEntity.ok(isFavorited);
    }

    @GetMapping
    public ResponseEntity<List<RentalHousing>> getUserFavorites(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        List<RentalHousing> favorites = favoriteService.getTopFavoriteRentals(user);
        return ResponseEntity.ok(favorites);
    }
}