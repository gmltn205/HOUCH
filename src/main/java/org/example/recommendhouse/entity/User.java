package org.example.recommendhouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "\"user\"")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "user_password", nullable = false)
    private String userPassword;

    private String region;

    @Column(name = "phone_num", unique = true)
    private String phoneNum;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "first_login")
    private boolean firstLogin = true; // 기본값을 true로 설정

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        firstLogin = true; // 생성 시 firstLogin을 true로 설정
    }
}