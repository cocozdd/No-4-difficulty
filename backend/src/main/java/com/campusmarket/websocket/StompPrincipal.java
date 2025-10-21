package com.campusmarket.websocket;

import java.security.Principal;

public class StompPrincipal implements Principal {

    private final Long userId;
    private final String username;
    private final String role;

    public StompPrincipal(Long userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}
