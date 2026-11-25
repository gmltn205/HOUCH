package org.example.recommendhouse.service;

import lombok.RequiredArgsConstructor;
import org.example.recommendhouse.dto.PropertyResponse;
import org.example.recommendhouse.dto.PropertyDetails;
import org.example.recommendhouse.dto.RecommendationRequest;
import org.example.recommendhouse.entity.User;
import org.example.recommendhouse.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final RestTemplate restTemplate;
    private final UserProfileService userProfileService;

    public List<PropertyResponse> getRecommendations(
            Map<String, Float> preferences,
            Long maxDeposit,
            Long maxMonthly,
            User user) {
        String url = "http://localhost:5001/api/recommend";

        // DB에서 로그인한 사용자의 실제 선호지역 조회
        List<String> userPreferredRegions = userProfileService.getPreferredRegionsByUser(user);
        System.out.println("🔍 사용자 ID: " + user.getUserId() + ", 조회된 선호지역: " + userPreferredRegions);

        RecommendationRequest request = new RecommendationRequest();
        request.setPreferences(preferences);
        request.setMaxDeposit(maxDeposit);
        request.setMaxMonthly(maxMonthly);
        request.setPreferredRegions(userPreferredRegions);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            System.out.println("Flask 서버 응답: " + response.getBody());  // 추가된 로그
            return convertToPropertyResponses(response.getBody());
        } catch (Exception e) {
            System.err.println("Flask 서버 호출 실패: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<PropertyResponse> convertToPropertyResponses(Map<String, List<Map<String, Object>>> result) {
        List<PropertyResponse> responses = new ArrayList<>();

        // 일반 추천 매물과 프리미엄 매물 모두 처리
        List<Map<String, Object>> recommended = (List<Map<String, Object>>) result.get("recommendedProperties");
        List<Map<String, Object>> premium = (List<Map<String, Object>>) result.get("premiumProperties");

        // 일반 추천 매물 변환
        if (recommended != null) {
            responses.addAll(convertProperties(recommended, false));
        }

        // 프리미엄 매물 변환
        if (premium != null) {
            responses.addAll(convertProperties(premium, true));
        }

        return responses;
    }

    private List<PropertyResponse> convertProperties(List<Map<String, Object>> properties, boolean isPremium) {
        return properties.stream().map(prop -> {
            PropertyResponse response = new PropertyResponse();

            // 기본 정보 설정
            response.setScore((float) prop.get("score"));
            response.setAddress((String) prop.get("address"));

            // 상세 점수 설정
            @SuppressWarnings("unchecked")
            Map<String, Float> detailScores = (Map<String, Float>) prop.get("detail_scores");
            response.setDetailScores(detailScores);

            // 매물 상세 정보 설정
            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) prop.get("details");
            PropertyDetails propertyDetails = new PropertyDetails();
            propertyDetails.setPropertyName((String) details.get("단지명"));
            propertyDetails.setUnitCount(((Number) details.get("세대수")).intValue());
            propertyDetails.setPropertyType((String) details.get("주택유형"));
            propertyDetails.setArea(((Number) details.get("공급면적(전용)")).floatValue());
            propertyDetails.setDeposit(((Number) details.get("임대보증금")).floatValue());
            propertyDetails.setMonthlyRent(((Number) details.get("월임대료")).floatValue());

            response.setDetails(propertyDetails);
            response.setPremium(isPremium);

            return response;
        }).collect(Collectors.toList());
    }
}