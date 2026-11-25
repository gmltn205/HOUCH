package org.example.recommendhouse.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;


@Controller
public class ChatbotPageController {

    @GetMapping("/chatbot")
    public String showChatbotPage() {
        return "chatbot";  // templates/chatbot.html을 렌더링
    }
}
