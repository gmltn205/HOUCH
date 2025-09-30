package org.example.recommendhouse.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class PropertyResponse {
    private String propertyName;
    private String address;
    private float score;
    private Map<String, Float> detailScores;
    private PropertyDetails details;
    private boolean isPremium;
}