package org.example.recommendhouse.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.recommendhouse.dto.RecommendationRequest;
import org.example.recommendhouse.dto.RecommendResponse;
import org.example.recommendhouse.dto.PropertyResponse;
import org.example.recommendhouse.dto.PropertyDetails;
import org.example.recommendhouse.dto.RecommendationRequest;
import org.example.recommendhouse.entity.User;
import org.example.recommendhouse.service.UserProfileService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecommendController {

    private final RestTemplate restTemplate;
    private final UserProfileService userProfileService;
    private static final String FLASK_API_URL = "http://localhost:5001/api/recommend";

    @PostMapping("/recommendations")
    public ResponseEntity<RecommendResponse> getRecommendations(@RequestBody RecommendationRequest request, HttpSession session) {
        try {
            // 세션에서 로그인한 사용자 정보 가져오기
            User user = (User) session.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new RecommendResponse());
            }

            // DB에서 로그인한 사용자의 실제 선호지역 조회하여 설정
            List<String> userPreferredRegions = userProfileService.getPreferredRegionsByUser(user);
            request.setPreferredRegions(userPreferredRegions);
            
            log.info("🔍 사용자 ID: {}, 조회된 선호지역: {}", user.getUserId(), userPreferredRegions);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<RecommendationRequest> requestEntity = new HttpEntity<>(request, headers);

            log.info("Flask 서버로 전송하는 데이터 - preferences: {}, maxDeposit: {}, maxMonthly: {}, preferredRegions: {}", 
                    request.getPreferences(), request.getMaxDeposit(), request.getMaxMonthly(), request.getPreferredRegions());

            ResponseEntity<Map> flaskResponse = restTemplate.exchange(
                    FLASK_API_URL,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            log.info("Flask 서버 응답 상태 코드: {}", flaskResponse.getStatusCode());
            log.info("Flask 서버 응답 본문: {}", flaskResponse.getBody());

            if (flaskResponse.getStatusCode() == HttpStatus.OK && flaskResponse.getBody() != null) {
                RecommendResponse response = processFlaskResponse(flaskResponse.getBody());
                log.info("처리된 응답: {}", response);
                return ResponseEntity.ok(response);
            } else {
                log.error("Flask 서버로부터 유효하지 않은 응답: {}", flaskResponse);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new RecommendResponse());
            }
        } catch (Exception e) {
            log.error("Flask 서버 통신 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RecommendResponse());
        }
    }

    private RecommendResponse processFlaskResponse(Map flaskResponse) {
        try {
            RecommendResponse response = new RecommendResponse();
            List<PropertyResponse> properties = new ArrayList<>();

            log.info("recommendedProperties 처리 시작");
            List<Map<String, Object>> recommended = (List<Map<String, Object>>)
                    flaskResponse.get("recommendedProperties");
            if (recommended != null) {
                log.info("일반 추천 매물 수: {}", recommended.size());
                properties.addAll(convertProperties(recommended, false));
            }

            log.info("premiumProperties 처리 시작");
            List<Map<String, Object>> premium = (List<Map<String, Object>>)
                    flaskResponse.get("premiumProperties");
            if (premium != null) {
                log.info("프리미엄 추천 매물 수: {}", premium.size());
                properties.addAll(convertProperties(premium, true));
            }

            response.setProperties(properties);
            log.info("전체 처리된 매물 수: {}", properties.size());
            return response;
        } catch (Exception e) {
            log.error("응답 처리 중 예외 발생", e);
            throw e;
        }
    }

    private List<PropertyResponse> convertProperties(List<Map<String, Object>> properties,
                                                     boolean isPremium) {
        return properties.stream().map(prop -> {
            try {
                PropertyResponse response = new PropertyResponse();
                response.setScore(((Number) prop.get("score")).floatValue());
                response.setAddress((String) prop.get("address"));
                response.setPremium(isPremium);

                // 상세 점수 처리
                @SuppressWarnings("unchecked")
                Map<String, Object> detailScores = (Map<String, Object>) prop.get("detail_scores");
                Map<String, Float> convertedScores = new HashMap<>();
                detailScores.forEach((key, value) ->
                        convertedScores.put(key, ((Number) value).floatValue())
                );
                response.setDetailScores(convertedScores);

                // 매물 상세 정보 처리
                @SuppressWarnings("unchecked")
                Map<String, Object> details = (Map<String, Object>) prop.get("details");
                if (details != null) {
                    PropertyDetails propertyDetails = new PropertyDetails(
                            (String) details.get("단지명"),
                            ((Number) details.get("세대수")).floatValue(),
                            (String) details.get("주택유형"),
                            ((Number) details.get("공급면적(전용)")).floatValue(),
                            ((Number) details.get("임대보증금")).floatValue(),
                            ((Number) details.get("월임대료")).floatValue()
                    );
                    response.setPropertyName(propertyDetails.getPropertyName());
                    response.setDetails(propertyDetails);
                }

                return response;
            } catch (Exception e) {
                log.error("매물 변환 중 오류 발생: {}", prop, e);
                throw new RuntimeException("매물 변환 실패", e);
            }
        }).collect(Collectors.toList());
    }
}