package com.campusmarket.dto;

public class AuthResponse {

    private final Long userId;
    private final String username;
    private final String token;
    private final String role;
    private final String nickname;
    private final long expiresInMs;

    public AuthResponse(Long userId,
                        String username,
                        String token,
                        String role,
                        String nickname,
                        long expiresInMs) {
        this.userId = userId;
        this.username = username;
        this.token = token;
        this.role = role;
        this.nickname = nickname;
        this.expiresInMs = expiresInMs;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getToken() {
        return token;
    }

    public String getRole() {
        return role;
    }

    public String getNickname() {
        return nickname;
    }

    public long getExpiresInMs() {
        return expiresInMs;
    }
}
