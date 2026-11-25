package org.example.recommendhouse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.recommendhouse.entity.Favorite;
import org.example.recommendhouse.entity.HouseInfo;
import org.example.recommendhouse.entity.User;
import org.example.recommendhouse.repository.FavoriteRepository;
import org.example.recommendhouse.repository.HouseInfoRepository;
import org.example.recommendhouse.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final HouseInfoRepository houseInfoRepository;

    // 찜 추가/제거 토글
    @Transactional
    public boolean toggleFavorite(Long userId, Long houseInfoId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        HouseInfo houseInfo = houseInfoRepository.findById(houseInfoId)
                .orElseThrow(() -> new RuntimeException("매물을 찾을 수 없습니다."));

        // 이미 찜한 상태인지 확인
        return favoriteRepository.findByUserAndHouseInfo_Id(user, houseInfoId)
                .map(favorite -> {
                    // 이미 찜한 경우 제거
                    favoriteRepository.delete(favorite);
                    return false;
                })
                .orElseGet(() -> {
                    // 찜하지 않은 경우 추가
                    Favorite favorite = new Favorite();
                    favorite.setUser(user);
                    favorite.setHouseInfo(houseInfo);
                    favoriteRepository.save(favorite);
                    return true;
                });
    }

    // 사용자의 찜 목록 상위 3개 조회
    public List<Favorite> getTopFavorites(User user) {

        List<Favorite> favorites = favoriteRepository.findTop3ByUserOrderByCreatedAtDesc(user);
        log.info("조회된 찜 목록: {}", favorites);

        return favoriteRepository.findTop3ByUserOrderByCreatedAtDesc(user);
    }

    // 사용자의 찜 목록 전체 조회 (페이징)
    public Page<Favorite> getAllFavorites(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return favoriteRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    // 특정 매물에 대한 찜 여부 확인
    public boolean isFavorite(User user, Long houseInfoId) {
        return favoriteRepository.findByUserAndHouseInfo_Id(user, houseInfoId).isPresent();
    }

    // 사용자의 찜 목록 전체 개수
    public long getFavoriteCount(User user) {
        return favoriteRepository.countByUser(user);
    }
}