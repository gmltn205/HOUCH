package org.example.recommendhouse.controller;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Logger;
import org.example.recommendhouse.dto.UserProfileRequest;
import org.example.recommendhouse.entity.User;
import org.example.recommendhouse.entity.UserProfile;
import org.example.recommendhouse.service.AuthService;
import org.example.recommendhouse.service.UserProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Slf4j  // 추가
@RestController
@RequestMapping("/api/user")
public class UserProfileController {
    private final UserProfileService userProfileService;
    private final AuthService authService;

    public UserProfileController(UserProfileService userProfileService, AuthService authService) {
        this.userProfileService = userProfileService;
        this.authService = authService;
    }

    @PostMapping("/profile")
    public ResponseEntity<?> saveUserProfile(@RequestBody UserProfileRequest request,
                                             HttpSession session) {

        try {
            User user = (User) session.getAttribute("user");

            // 디버깅을 위한 로그 추가
            log.info("Received profile request: {}", request);
            log.info("Current user from session: {}", user);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
            }

            // 프로필 저장
            UserProfile profile = userProfileService.saveProfile(user, request);

            // firstLogin 상태 업데이트
            authService.updateFirstLoginStatus(user.getUserId());

            return ResponseEntity.ok("프로필이 저장되었습니다.");
        } catch (Exception e) {
            log.error("Error saving profile", e);  // 에러 로그 추가
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}