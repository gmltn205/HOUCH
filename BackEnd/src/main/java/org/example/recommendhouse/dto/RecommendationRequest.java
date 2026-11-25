package org.example.recommendhouse.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

// DTO 클래스들
@Getter
@Setter
public class RecommendationRequest {
    private Map<String, Float> preferences;
    private long maxDeposit;
    private long maxMonthly;
    private List<String> preferredRegions;
}