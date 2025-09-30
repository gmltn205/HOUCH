package org.example.recommendhouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "house_info")
@Getter
@Setter
public class HouseInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rental_type", nullable = true)  // nullable = true로 변경
    private String rentalType;

    @Column(name = "city", nullable = true)
    private String city;

    @Column(name = "district", nullable = true)
    private String district;

    @Column(name = "road_address", nullable = true)
    private String roadAddress;

    @Column(name = "complex_name", nullable = true)
    private String complexName;

    @Column(name = "house_type", nullable = true)
    private String houseType;

    @Column(name = "building_type", nullable = true)
    private String buildingType;

    @Column(name = "heating_type", nullable = true)
    private String heatingType;

    @Column(name = "has_elevator", nullable = true)
    private String hasElevator;

    @Column(name = "model_name", nullable = true)
    private String modelName;

    @Column(name = "exclusive_area", nullable = true)
    private Double exclusiveArea;

    @Column(name = "common_area", nullable = true)
    private Double commonArea;

    @Column(name = "deposit", nullable = true)
    private Long deposit;

    @Column(name = "monthly_rent", nullable = true)
    private Long monthlyRent;

    @Column(nullable = true)
    private Double latitude;

    @Column(nullable = true)
    private Double longitude;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L; // 초기값 0으로 설정
}