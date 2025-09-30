package org.example.recommendhouse.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class UserProfileRequest {
    private List<String> preferredRegions;
    private String gender;
    private String ageGroup;
    private Long annualIncome;
    private String personalCharacteristic;
    private String householdCharacteristic;
}