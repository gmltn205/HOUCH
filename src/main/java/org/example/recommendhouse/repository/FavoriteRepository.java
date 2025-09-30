package org.example.recommendhouse.repository;

import org.example.recommendhouse.entity.Favorite;
import org.example.recommendhouse.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    // 사용자의 찜 목록 조회 (페이징)
    Page<Favorite> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // 사용자의 찜 목록 상위 3개 조회
    List<Favorite> findTop3ByUserOrderByCreatedAtDesc(User user);

    // 특정 사용자의 특정 매물에 대한 찜 여부 확인
    Optional<Favorite> findByUserAndHouseInfo_Id(User user, Long houseInfoId);

    // 특정 사용자의 찜 목록 전체 개수
    long countByUser(User user);
}