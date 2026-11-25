package org.example.recommendhouse.service;


import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.example.recommendhouse.entity.HouseInfo;
import org.example.recommendhouse.repository.HouseInfoRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
@Service
@RequiredArgsConstructor
public class HouseInfoService {
    private final HouseInfoRepository houseInfoRepository;


    // 조회수 증가
    public void incrementViewCount(Long id) {
        HouseInfo house = houseInfoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("매물을 찾을 수 없습니다: " + id));
        house.setViewCount(house.getViewCount() + 1);
        houseInfoRepository.save(house);
    }

    public Page<HouseInfo> getFilteredHousesByViewCount(
            String keyword, String city, String houseType,
            Long minDeposit, Long maxDeposit, Pageable pageable) {

        return houseInfoRepository.findFilteredHousesByViewCount(
                keyword, city, houseType, minDeposit, maxDeposit, pageable);
    }

    // 인기 매물 Top 5 조회
    public List<HouseInfo> getTopViewedHouses() {
        return houseInfoRepository.findTop5ByDepositGreaterThanOrMonthlyRentGreaterThanOrderByViewCountDescIdAsc(0L, 0L);
    }
    public Page<HouseInfo> getHousesByViewCount(Pageable pageable) {
        return houseInfoRepository.findAllByOrderByViewCountDescIdAsc(pageable);
    }
    // 상세 정보 조회 시 조회수도 함께 증가
    public HouseInfo getHouseById(Long id) {
        HouseInfo house = houseInfoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("매물을 찾을 수 없습니다: " + id));
        incrementViewCount(id);
        return house;
    }
    @PostConstruct
    @Transactional
    public void importCsvData() {
        if (houseInfoRepository.count() > 0) {
            return;
        }

        try {
            Resource resource = new ClassPathResource("lh_happyhouse_list_with_coords.csv");
            Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);

            // CSV 파싱 설정 수정
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase()
                    .withTrim());

            List<HouseInfo> houseInfoList = new ArrayList<>();

            for (CSVRecord record : csvParser) {
                try {
                    HouseInfo houseInfo = new HouseInfo();
                    // 각 필드의 존재 여부를 확인하고 설정
                    if (record.isMapped("임대종류")) {
                        houseInfo.setRentalType(record.get("임대종류"));
                    }
                    if (record.isMapped("광역시도")) {
                        houseInfo.setCity(record.get("광역시도"));
                    }
                    if (record.isMapped("시군구")) {
                        houseInfo.setDistrict(record.get("시군구"));
                    }
                    if (record.isMapped("도로명주소")) {
                        houseInfo.setRoadAddress(record.get("도로명주소"));
                    }
                    if (record.isMapped("단지명")) {
                        houseInfo.setComplexName(record.get("단지명"));
                    }
                    if (record.isMapped("주택유형")) {
                        houseInfo.setHouseType(record.get("주택유형"));
                    }
                    if (record.isMapped("건물형태")) {
                        houseInfo.setBuildingType(record.get("건물형태"));
                    }
                    if (record.isMapped("난방방식")) {
                        houseInfo.setHeatingType(record.get("난방방식"));
                    }
                    if (record.isMapped("승강기설치여부")) {
                        houseInfo.setHasElevator(record.get("승강기설치여부"));
                    }
                    if (record.isMapped("형명")) {
                        houseInfo.setModelName(record.get("형명"));
                    }
                    if (record.isMapped("공급면적(전용)")) {
                        houseInfo.setExclusiveArea(Double.parseDouble(record.get("공급면적(전용)")));
                    }
                    if (record.isMapped("공급면적(공용)")) {
                        houseInfo.setCommonArea(Double.parseDouble(record.get("공급면적(공용)")));
                    }
                    if (record.isMapped("임대보증금")) {
                        houseInfo.setDeposit(Long.parseLong(record.get("임대보증금")));
                    }
                    if (record.isMapped("월임대료")) {
                        houseInfo.setMonthlyRent(Long.parseLong(record.get("월임대료")));
                    }
                    if (record.isMapped("위도")) {
                        houseInfo.setLatitude(Double.parseDouble(record.get("위도")));
                    }
                    if (record.isMapped("경도")) {
                        houseInfo.setLongitude(Double.parseDouble(record.get("경도")));
                    }

                    houseInfoList.add(houseInfo);
                } catch (Exception e) {
                    System.out.println("Error processing record: " + record.toString());
                    e.printStackTrace();
                }
            }

            // 데이터 일괄 저장
            houseInfoRepository.saveAll(houseInfoList);

        } catch (IOException e) {
            throw new RuntimeException("CSV 파일 처리 중 오류 발생", e);
        }
    }
    // 전체 주택 정보 조회
    public List<HouseInfo> getAllHouses() {
        return houseInfoRepository.findAll();
    }

    // 도시별 주택 조회
    public List<HouseInfo> getHousesByCity(String city) {
        return houseInfoRepository.findByCity(city);
    }

    // 주택 유형별 조회
    public List<HouseInfo> getHousesByType(String houseType) {
        return houseInfoRepository.findByHouseType(houseType);
    }

    // 조건 검색 (도시, 주택유형, 면적 범위)
    public List<HouseInfo> searchHouses(String city, String houseType, Double minArea, Double maxArea) {
        return houseInfoRepository.findByCityAndHouseTypeAndExclusiveAreaBetween(
                city, houseType, minArea, maxArea
        );
    }

    // 보증금 범위로 검색
    public List<HouseInfo> getHousesByMaxDeposit(Long maxDeposit) {
        return houseInfoRepository.findByDepositLessThanEqual(maxDeposit);
    }

    // 위치 기반 검색 (위도, 경도 범위 내)
    public List<HouseInfo> getHousesByLocation(Double lat, Double lon, Double radiusKm) {
        // 위도 1도 = 약 111km, 경도 1도 = 약 88.8km (한반도 기준)
        Double latRange = radiusKm / 111.0;
        Double lonRange = radiusKm / 88.8;

        return houseInfoRepository.findByLatitudeBetweenAndLongitudeBetween(
                lat - latRange, lat + latRange,
                lon - lonRange, lon + lonRange
        );
    }

}