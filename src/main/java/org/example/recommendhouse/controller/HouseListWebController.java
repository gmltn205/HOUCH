package org.example.recommendhouse.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HouseListWebController {

    @GetMapping("/houses")
    public String housesPage() {
        return "houselist";  // houselist.html을 렌더링
    }

}