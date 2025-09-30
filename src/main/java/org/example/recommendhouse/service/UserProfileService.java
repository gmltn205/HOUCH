package org.example.recommendhouse.service;

import jakarta.transaction.Transactional;
import org.example.recommendhouse.dto.UserProfileRequest;
import org.example.recommendhouse.entity.User;
import org.example.recommendhouse.entity.UserProfile;
import org.example.recommendhouse.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfile saveProfile(User user, UserProfileRequest request) {
        // 선호 지역 개수 검증 (최대 3개)
        if (request.getPreferredRegions() != null && request.getPreferredRegions().size() > 3) {
            throw new IllegalArgumentException("선호 지역은 최대 3개까지만 선택할 수 있습니다.");
        }

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setPreferredRegions(request.getPreferredRegions());
        profile.setGender(request.getGender());
        profile.setAgeGroup(request.getAgeGroup());
        profile.setAnnualIncome(request.getAnnualIncome());
        profile.setPersonalCharacteristic(request.getPersonalCharacteristic());
        profile.setHouseholdCharacteristic(request.getHouseholdCharacteristic());

        return userProfileRepository.save(profile);
    }

    public List<String> getPreferredRegionsByUser(User user) {
        return userProfileRepository.findByUser(user)
                .map(UserProfile::getPreferredRegions)
                .orElse(new ArrayList<>());
    }
}