package org.example.recommendhouse.service;

import jakarta.transaction.Transactional;
import org.example.recommendhouse.dto.AuthDtos.LoginRequest;
import org.example.recommendhouse.dto.AuthDtos.RegisterRequest;
import org.example.recommendhouse.entity.User;
import org.example.recommendhouse.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {
        // 중복 검사
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("이미 존재하는 아이디입니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        // 새 사용자 생성
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setUserPassword(passwordEncoder.encode(request.getPassword()));

        return userRepository.save(user);
    }

    public User login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getUserPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        return user;
    }
    @Transactional
    public void updateFirstLoginStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.setFirstLogin(false);
        userRepository.save(user);
    }
}