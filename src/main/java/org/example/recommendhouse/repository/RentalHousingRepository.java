package org.example.recommendhouse.repository;

import org.example.recommendhouse.entity.RentalHousing;
import org.example.recommendhouse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RentalHousingRepository extends JpaRepository<RentalHousing, Long> {
    Optional<RentalHousing> findByHouseManageNo(String houseManageNo);

    // 사용자의 찜한 청약 매물 조회
    @Query("SELECT r FROM RentalHousing r WHERE r.user = :user AND r.favorited = true")
    List<RentalHousing> findByUserAndFavoritedTrue(@Param("user") User user);

    // 특정 사용자의 특정 청약 매물 찾기
    Optional<RentalHousing> findByHouseManageNoAndUser(String houseManageNo, User user);

    // 사용자의 찜한 청약 매물 개수 조회
    @Query("SELECT COUNT(r) FROM RentalHousing r WHERE r.user = :user AND r.favorited = true")
    long countByUserAndFavoritedTrue(@Param("user") User user);
}