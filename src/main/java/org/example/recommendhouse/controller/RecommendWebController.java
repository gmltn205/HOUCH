package org.example.recommendhouse.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RecommendWebController {
    @GetMapping("/recommend")
    public String recommendPage() {
        return "recommend";
    }
}

