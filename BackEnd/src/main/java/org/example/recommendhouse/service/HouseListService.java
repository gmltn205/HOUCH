package org.example.recommendhouse.service;

import lombok.RequiredArgsConstructor;
import org.example.recommendhouse.dto.HouseListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HouseListService {

    private final JdbcTemplate jdbcTemplate;

    public Page<HouseListResponse> getHouseList(
            String keyword, String city, String houseType,
            Double minArea, Double maxArea, Long minDeposit, Long maxDeposit,
            Long minMonthly, Long maxMonthly, Pageable pageable) {

        StringBuilder sql = new StringBuilder(
                "SELECT * FROM house_info WHERE 1=1");
        StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM house_info WHERE 1=1");
        List<Object> params = new ArrayList<>();
        List<Object> countParams = new ArrayList<>();

        // 검색어 필터
        if (keyword != null && !keyword.trim().isEmpty()) {
            String whereClause = " AND (complex_name LIKE ? OR road_address LIKE ?)";
            sql.append(whereClause);
            countSql.append(whereClause);
            String likeKeyword = "%" + keyword + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
            countParams.add(likeKeyword);
            countParams.add(likeKeyword);
        }

        // 도시 필터
        if (city != null && !city.trim().isEmpty()) {
            String whereClause = " AND district = ?";
            sql.append(whereClause);
            countSql.append(whereClause);
            params.add(city);
            countParams.add(city);
        }

        // 주택유형 필터
        if (houseType != null && !houseType.trim().isEmpty()) {
            String whereClause = " AND house_type = ?";
            sql.append(whereClause);
            countSql.append(whereClause);
            params.add(houseType);
            countParams.add(houseType);
        }

        // 면적 필터
        if (minArea != null) {
            String whereClause = " AND exclusive_area >= ?";
            sql.append(whereClause);
            countSql.append(whereClause);
            params.add(minArea);
            countParams.add(minArea);
        }
        if (maxArea != null) {
            String whereClause = " AND exclusive_area <= ?";
            sql.append(whereClause);
            countSql.append(whereClause);
            params.add(maxArea);
            countParams.add(maxArea);
        }

        // 보증금 필터
        if (minDeposit != null) {
            String whereClause = " AND deposit >= ?";
            sql.append(whereClause);
            countSql.append(whereClause);
            params.add(minDeposit);
            countParams.add(minDeposit);
        }
        if (maxDeposit != null) {
            String whereClause = " AND deposit <= ?";
            sql.append(whereClause);
            countSql.append(whereClause);
            params.add(maxDeposit);
            countParams.add(maxDeposit);
        }

        // 월세 필터
        if (minMonthly != null) {
            String whereClause = " AND monthly_rent >= ?";
            sql.append(whereClause);
            countSql.append(whereClause);
            params.add(minMonthly);
            countParams.add(minMonthly);
        }
        if (maxMonthly != null) {
            String whereClause = " AND monthly_rent <= ?";
            sql.append(whereClause);
            countSql.append(whereClause);
            params.add(maxMonthly);
            countParams.add(maxMonthly);
        }

        // 정렬
        sql.append(" ORDER BY view_count DESC, id ASC");

        // 페이징
        sql.append(" LIMIT ? OFFSET ?");
        params.add(pageable.getPageSize());
        params.add(pageable.getOffset());

        // 전체 개수 조회
        int totalCount = jdbcTemplate.queryForObject(countSql.toString(), countParams.toArray(), Integer.class);

        // 데이터 조회
        List<HouseListResponse> content = jdbcTemplate.query(
                sql.toString(),
                params.toArray(),
                (rs, rowNum) -> new HouseListResponse(
                        rs.getLong("id"),
                        rs.getString("complex_name"),
                        rs.getString("road_address"),
                        rs.getString("city"),
                        rs.getString("district"),
                        rs.getString("house_type"),
                        rs.getDouble("exclusive_area"),
                        rs.getLong("deposit"),
                        rs.getLong("monthly_rent"),
                        null, // households 컬럼이 없으므로 null
                        rs.getString("heating_type"),
                        rs.getString("has_elevator").equals("Y"),
                        null, // parking_spaces 컬럼이 없으므로 null
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")
                )
        );

        return new PageImpl<>(content, pageable, totalCount);
    }

    public List<String> getAllCities() {
        String sql = "SELECT DISTINCT city FROM house_info ORDER BY city";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    public List<String> getAllHouseTypes() {
        String sql = "SELECT DISTINCT house_type FROM house_info ORDER BY house_type";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    public Page<HouseListResponse> getPopularHouses(Pageable pageable) {
        String countSql = "SELECT COUNT(*) FROM house_info";
        int totalCount = jdbcTemplate.queryForObject(countSql, Integer.class);

        String sql = "SELECT * FROM house_info " +
                "ORDER BY view_count DESC, id ASC " +
                "LIMIT ? OFFSET ?";

        List<HouseListResponse> content = jdbcTemplate.query(
                sql,
                new Object[]{pageable.getPageSize(), pageable.getOffset()},
                (rs, rowNum) -> new HouseListResponse(
                        rs.getLong("id"),
                        rs.getString("complex_name"),
                        rs.getString("road_address"),
                        rs.getString("city"),
                        rs.getString("district"),
                        rs.getString("house_type"),
                        rs.getDouble("exclusive_area"),
                        rs.getLong("deposit"),
                        rs.getLong("monthly_rent"),
                        null,
                        rs.getString("heating_type"),
                        rs.getString("has_elevator").equals("Y"),
                        null,
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")
                )
        );

        return new PageImpl<>(content, pageable, totalCount);
    }
}