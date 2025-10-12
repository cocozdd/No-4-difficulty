package com.campusmarket.dto;

public class UserProfileResponse {
    private Long id;
    private String username;
    private String nickname;
    private String role;
    private String avatarUrl;
    private String phone;

    public UserProfileResponse(Long id, String username, String nickname, String role, String avatarUrl, String phone) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.role = role;
        this.avatarUrl = avatarUrl;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public String getRole() {
        return role;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getPhone() {
        return phone;
    }
}
