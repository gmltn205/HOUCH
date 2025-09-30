package org.example.recommendhouse.service;

import lombok.RequiredArgsConstructor;
import org.example.recommendhouse.entity.RentalHousing;
import org.example.recommendhouse.entity.User;
import org.example.recommendhouse.repository.RentalHousingRepository;
import org.example.recommendhouse.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class favorite_cheongyak {
    private final RentalHousingRepository rentalHousingRepository;
    private final UserRepository userRepository;
    private final ApplyhomeService applyhomeService; // ApplyhomeService 추가

    @Transactional
    public boolean toggleFavorite(String houseManageNo, User user) {
        if (user == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }

        Optional<RentalHousing> existingFavorite = rentalHousingRepository
                .findByHouseManageNoAndUser(houseManageNo, user);

        if (existingFavorite.isPresent()) {
            rentalHousingRepository.delete(existingFavorite.get());
            return false;
        } else {
            // API에서 해당 매물의 상세 정보를 가져옴
            List<RentalHousing> rentals = applyhomeService.getGyeonggiPublicRentals();
            RentalHousing rentalInfo = rentals.stream()
                    .filter(r -> r.getHouseManageNo().equals(houseManageNo))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("매물 정보를 찾을 수 없습니다."));

            // 새로운 찜 객체 생성 및 모든 정보 복사
            RentalHousing newFavorite = new RentalHousing();
            newFavorite.setHouseManageNo(houseManageNo);
            newFavorite.setUser(user);
            newFavorite.setFavorited(true);

            // 기본 정보 복사
            newFavorite.setHouseName(rentalInfo.getHouseName());
            newFavorite.setAddress(rentalInfo.getAddress());
            newFavorite.setTotalSupplyCount(rentalInfo.getTotalSupplyCount());
            newFavorite.setRecruitDate(rentalInfo.getRecruitDate());
            newFavorite.setSubscriptionStartDate(rentalInfo.getSubscriptionStartDate());
            newFavorite.setSubscriptionEndDate(rentalInfo.getSubscriptionEndDate());
            newFavorite.setWinnerAnnouncementDate(rentalInfo.getWinnerAnnouncementDate());
            newFavorite.setHomepageUrl(rentalInfo.getHomepageUrl());
            newFavorite.setBusinessName(rentalInfo.getBusinessName());
            newFavorite.setContactNumber(rentalInfo.getContactNumber());

            rentalHousingRepository.save(newFavorite);
            return true;
        }
    }

    @Transactional(readOnly = true)
    public List<RentalHousing> getTopFavoriteRentals(User user) {
        return rentalHousingRepository.findByUserAndFavoritedTrue(user).stream()
                .limit(3)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getFavoriteRentalCount(User user) {
        return rentalHousingRepository.findByUserAndFavoritedTrue(user).size();
    }

    // 청약 매물 찜 해제
    @Transactional
    public boolean toggleRentalFavorite(String houseManageNo, User user) {
        Optional<RentalHousing> existingFavorite = rentalHousingRepository
                .findByHouseManageNoAndUser(houseManageNo, user);

        if (existingFavorite.isPresent()) {
            rentalHousingRepository.delete(existingFavorite.get());
            return false;
        }

        return true;
    }
}