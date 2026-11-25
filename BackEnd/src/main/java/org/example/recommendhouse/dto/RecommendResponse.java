package org.example.recommendhouse.dto;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
public class RecommendResponse {
    private List<PropertyResponse> properties = new ArrayList<>();
}

