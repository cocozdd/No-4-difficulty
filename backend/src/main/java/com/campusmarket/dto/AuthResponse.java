package com.campusmarket.dto;

public class AuthResponse {
    private String token;
    private String role;
    private String nickname;

    public AuthResponse(String token, String role, String nickname) {
        this.token = token;
        this.role = role;
        this.nickname = nickname;
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
}
