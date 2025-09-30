package org.example.recommendhouse.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CommunityController {
    @GetMapping("/community")
    public String showMyPage() {
        // templates/home.html 파일 렌더링
        return "community";
    }
}
