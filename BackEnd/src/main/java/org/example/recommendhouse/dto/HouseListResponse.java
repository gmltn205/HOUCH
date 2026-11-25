package org.example.recommendhouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HouseListResponse {
    private Long id;
    private String complexName;        // 단지명
    private String address;            // 도로명주소
    private String city;               // 광역시도
    private String district;           // 시군구
    private String houseType;          // 주택유형
    private Double area;               // 공급면적(전용)
    private Long deposit;              // 임대보증금
    private Long monthlyRent;          // 월임대료
    private Integer households;        // 세대수
    private String heatingType;        // 난방방식
    private Boolean hasElevator;       // 승강기설치여부
    private Integer parkingSpaces;     // 주차수
    private Double latitude;           // 위도
    private Double longitude;          // 경도
}