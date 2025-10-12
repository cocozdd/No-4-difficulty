package com.campusmarket.service;

import com.campusmarket.dto.AuthRequest;
import com.campusmarket.dto.AuthResponse;
import com.campusmarket.dto.RegisterRequest;
import com.campusmarket.dto.UserProfileResponse;
import com.campusmarket.entity.User;

public interface UserService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(AuthRequest request);
    UserProfileResponse getProfile(Long userId);
    User findById(Long id);
    User findByUsername(String username);
}
