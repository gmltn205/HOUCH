package org.example.recommendhouse.controller;

import lombok.RequiredArgsConstructor;
import org.example.recommendhouse.service.FilteredRecommendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot/recommend")
@RequiredArgsConstructor
public class FilteredRecommendController {

    private final FilteredRecommendService filteredRecommendService;

    @PostMapping
    public ResponseEntity<Map<String, String>> processRecommendation(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        String recommendation = filteredRecommendService.generateFilteredRecommendation(userMessage);

        Map<String, String> response = new HashMap<>();
        response.put("response", recommendation);

        return ResponseEntity.ok(response);
    }
}