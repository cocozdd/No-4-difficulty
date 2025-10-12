package com.campusmarket.controller;

import com.campusmarket.dto.AuthRequest;
import com.campusmarket.dto.AuthResponse;
import com.campusmarket.dto.RegisterRequest;
import com.campusmarket.dto.UserProfileResponse;
import com.campusmarket.entity.User;
import com.campusmarket.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody @Validated RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Validated AuthRequest request) {
        return userService.login(request);
    }

    @GetMapping("/profile")
    public UserProfileResponse profile(@AuthenticationPrincipal User currentUser) {
        return userService.getProfile(currentUser.getId());
    }
}
