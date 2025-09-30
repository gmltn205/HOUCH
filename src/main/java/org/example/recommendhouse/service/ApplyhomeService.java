package org.example.recommendhouse.service;

import lombok.extern.slf4j.Slf4j;
import org.example.recommendhouse.dto.ApiResponse;
import org.example.recommendhouse.entity.RentalHousing;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// 청약 임대 리스트 보기
@Service
@Slf4j
public class ApplyhomeService {
    private final RestTemplate restTemplate;

    public ApplyhomeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<RentalHousing> getGyeonggiPublicRentals() {
        try {
            String url = "https://api.odcloud.kr/api/ApplyhomeInfoDetailSvc/v1/getRemndrLttotPblancDetail?page=1&perPage=100000&serviceKey=NbgvcGOvtRgvFU0Gt7r6aAg1tx7Fmk9xFUL2hQpENQ8Ln1k26rgPXNIdK69iK2kKvqVHg1S1kL2yTmmPOWDerw==";

            ResponseEntity<ApiResponse> response = restTemplate.getForEntity(url, ApiResponse.class);
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            if (response.getBody() != null && response.getBody().getData() != null) {
                // 경기도 데이터만 필터링
                return response.getBody().getData().stream()
                        .filter(housing -> {
                            // 주소에 "경기" 포함되어 있는지 확인
                            String address = housing.getAddress();
                            String endDateStr = housing.getSubscriptionEndDate();
                            return address != null && address.contains("경기") && endDateStr.compareTo(today) >=0 ;

                        })
                        .collect(Collectors.toList());
            }

        } catch (Exception e) {
            log.error("API call failed", e);
            log.error("Error message: {}", e.getMessage());
        }

        return new ArrayList<>();
    }
}