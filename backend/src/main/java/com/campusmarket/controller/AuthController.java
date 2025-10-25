package com.campusmarket.controller;

import com.campusmarket.dto.AuthRequest;
import com.campusmarket.dto.AuthResponse;
import com.campusmarket.dto.RegisterRequest;
import com.campusmarket.dto.UserProfileResponse;
import com.campusmarket.entity.User;
import com.campusmarket.security.jwt.JwtTokenProvider;
import com.campusmarket.service.LoginSessionService;
import com.campusmarket.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final LoginSessionService loginSessionService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(UserService userService,
                          LoginSessionService loginSessionService,
                          JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.loginSessionService = loginSessionService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody @Validated RegisterRequest request,
                                 HttpServletRequest servletRequest) {
        AuthResponse response = userService.register(request);
        storeSession(response, servletRequest);
        return response;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Validated AuthRequest request,
                              HttpServletRequest servletRequest) {
        AuthResponse response = userService.login(request);
        storeSession(response, servletRequest);
        return response;
    }

    @GetMapping("/profile")
    public UserProfileResponse profile(@AuthenticationPrincipal User currentUser) {
        return userService.getProfile(currentUser.getId());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            loginSessionService.revokeSession(token);
        }
    }

    private void storeSession(AuthResponse response, HttpServletRequest request) {
        String token = response.getToken();
        if (!StringUtils.hasText(token)) {
            return;
        }
        String ip = resolveClientIp(request);
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        loginSessionService.storeSession(
                token,
                response.getUserId(),
                response.getUsername(),
                response.getRole(),
                jwtTokenProvider.getExpirationMs(),
                ip,
                userAgent
        );
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
