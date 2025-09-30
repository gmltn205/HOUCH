package org.example.recommendhouse.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@RestController
@RequestMapping("/api/flask")
@CrossOrigin(origins = "http://localhost:5001")
public class FlaskRecommendationController {

    private final RestTemplate restTemplate;

    public FlaskRecommendationController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/recommend")
    public ResponseEntity<?> getRecommendations(@RequestBody Map<String, Object> requestData) {
        String flaskUrl = "http://127.0.0.1:5001/api/recommend"; // Flask 서버 주소

        // HTTP 요청 헤더 설정 (JSON 형식)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 요청 객체 생성
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestData, headers);

        try {
            // Flask 서버에 요청 보내기
            ResponseEntity<Map> response = restTemplate.exchange(flaskUrl, HttpMethod.POST, requestEntity, Map.class);

            // Flask에서 받은 응답을 그대로 클라이언트(프론트엔드)에게 반환
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Flask 서버 호출 실패: " + e.getMessage());
        }
    }
}
