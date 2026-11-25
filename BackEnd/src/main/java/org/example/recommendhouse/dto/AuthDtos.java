package org.example.recommendhouse.dto;

import lombok.Getter;
import lombok.Setter;

public class AuthDtos {
    @Getter
    @Setter
    public static class RegisterRequest {
        private String email;
        private String username;
        private String name;
        private String Password;
    }

    @Getter
    @Setter
    public static class LoginRequest {
        private String username;
        private String Password;
    }

    @Getter
    @Setter
    public static class AuthResponse {
        private String token;
        private String username;

        public AuthResponse(String token, String username) {
            this.token = token;
            this.username = username;
        }
    }
}