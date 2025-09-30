package org.example.recommendhouse.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.recommendhouse.dto.PropertyResponse;
import org.example.recommendhouse.dto.RecommendationRequest;
import org.example.recommendhouse.dto.RecommendationRequest;
import org.example.recommendhouse.entity.User;
import org.example.recommendhouse.service.RecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Controller 클래스
@RestController
@CrossOrigin(origins = "*")  // 모든 도메인 허용
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {
    private final RecommendationService recommendationService;

    @PostMapping("/recommend")
    public ResponseEntity<?> getRecommend(
            @RequestBody RecommendationRequest request, 
            HttpSession session) {
        try {
            // 세션에서 로그인한 사용자 정보 가져오기
            User user = (User) session.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("로그인이 필요합니다.");
            }

            List<PropertyResponse> recommendations =
                    recommendationService.getRecommendations(
                            request.getPreferences(),
                            (long) request.getMaxDeposit(),
                            (long) request.getMaxMonthly(),
                            user
                    );
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error occurred: " + e.getMessage());
        }
    }
}