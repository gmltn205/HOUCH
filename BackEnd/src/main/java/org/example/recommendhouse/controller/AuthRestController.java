package org.example.recommendhouse.controller;

import jakarta.servlet.http.HttpSession;
import org.example.recommendhouse.dto.AuthDtos.LoginRequest;
import org.example.recommendhouse.dto.AuthDtos.RegisterRequest;
import org.example.recommendhouse.dto.AuthDtos.AuthResponse;
import org.example.recommendhouse.entity.User;
import org.example.recommendhouse.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {
    private final AuthService authService;

    public AuthRestController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            User user = authService.register(request);
            Map<String, String> response = new HashMap<>();
            response.put("message", "회원가입이 완료되었습니다.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session) {
        try {
            User user = authService.login(request);
            session.setAttribute("user", user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그인이 완료되었습니다.");
            response.put("username", user.getUsername());
            response.put("firstLogin", user.isFirstLogin());  // 첫 로그인 여부 추가

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        Map<String, String> response = new HashMap<>();
        response.put("message", "로그아웃 되었습니다.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<?> getLoginStatus(HttpSession session) {
        User user = (User) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        response.put("isLoggedIn", user != null);
        if (user != null) {
            response.put("username", user.getUsername());
        }

        return ResponseEntity.ok(response);
    }
}