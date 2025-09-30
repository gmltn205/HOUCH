package org.example.recommendhouse.repository;

import org.example.recommendhouse.entity.HouseInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HouseInfoRepository extends JpaRepository<HouseInfo, Long> {
    List<HouseInfo> findByCity(String city);

    List<HouseInfo> findByHouseType(String houseType);

    List<HouseInfo> findByDepositLessThanEqual(Long maxDeposit);

    List<HouseInfo> findByCityAndHouseTypeAndExclusiveAreaBetween(
            String city, String houseType, Double minArea, Double maxArea
    );

    List<HouseInfo> findByLatitudeBetweenAndLongitudeBetween(
            Double latStart, Double latEnd,
            Double lonStart, Double lonEnd
    );
    List<HouseInfo> findTop5ByDepositGreaterThanOrMonthlyRentGreaterThanOrderByViewCountDescIdAsc(
            Long depositThreshold,
            Long monthlyRentThreshold
    );
    Page<HouseInfo> findAllByOrderByViewCountDescIdAsc(Pageable pageable);
    @Query("SELECT h FROM HouseInfo h " +
            "WHERE (:keyword IS NULL OR h.complexName LIKE %:keyword% OR h.roadAddress LIKE %:keyword%) " +
            "AND (:city IS NULL OR h.city = :city) " +
            "AND (:houseType IS NULL OR h.houseType = :houseType) " +
            "AND (:minDeposit IS NULL OR h.deposit >= :minDeposit) " +
            "AND (:maxDeposit IS NULL OR h.deposit <= :maxDeposit) " +
            "ORDER BY h.viewCount DESC, h.id ASC")
    Page<HouseInfo> findFilteredHousesByViewCount(
            @Param("keyword") String keyword,
            @Param("city") String city,
            @Param("houseType") String houseType,
            @Param("minDeposit") Long minDeposit,
            @Param("maxDeposit") Long maxDeposit,
            Pageable pageable
    );
}