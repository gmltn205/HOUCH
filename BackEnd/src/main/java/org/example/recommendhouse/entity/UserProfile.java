package org.example.recommendhouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "user_profile")
@Getter
@Setter
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ElementCollection
    @CollectionTable(name = "preferred_regions", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "region")
    private List<String> preferredRegions;

    @Column(nullable = false)
    private String gender;

    @Column(nullable = false)
    private String ageGroup;

    @Column(nullable = false)
    private Long annualIncome;  // 만원 단위

    @Column(nullable = false)
    private Double medianIncomePercentage;  // 중위소득 퍼센트

    @Column(nullable = false)
    private String personalCharacteristic;  // 개인특성

    @Column(nullable = false)
    private String householdCharacteristic;  // 가구특성

    @PrePersist
    protected void onCreate() {
        calculateMedianIncomePercentage();
    }

    private void calculateMedianIncomePercentage() {
        // 2024년 기준 중위소득 (4인 가구 기준: 5,429,914원/월)
        double monthlyMedianIncome = 5429914;
        double annualMedianIncome = monthlyMedianIncome * 12;

        this.medianIncomePercentage = (this.annualIncome * 10000 / annualMedianIncome) * 100;
    }
}