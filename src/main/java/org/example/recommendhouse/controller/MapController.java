package org.example.recommendhouse.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MapController {
    @GetMapping("/map")
    public String mapPage() {
        return "map";  // templates/map.html을 렌더링
    }
}