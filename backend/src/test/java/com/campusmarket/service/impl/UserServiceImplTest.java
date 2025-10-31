package com.campusmarket.service.impl;

import com.campusmarket.dto.AuthRequest;
import com.campusmarket.entity.User;
import com.campusmarket.mapper.UserMapper;
import com.campusmarket.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userMapper, passwordEncoder, jwtTokenProvider, redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void loginShouldBeBlockedWhenUserLocked() {
        String username = "lockedUser";
        when(redisTemplate.hasKey("auth:lock:" + username)).thenReturn(true);

        AuthRequest request = new AuthRequest();
        request.setUsername(username);
        request.setPassword("any");

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("登录失败次数过多");

        verify(redisTemplate).hasKey("auth:lock:" + username);
        verifyNoInteractions(userMapper);
    }

    @Test
    void loginShouldRecordFailureAndExpireCounter() {
        String username = "demo";
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPassword("hash");
        user.setRole("STUDENT");
        user.setNickname("Demo");
        user.setCreatedAt(LocalDateTime.now());

        when(redisTemplate.hasKey("auth:lock:" + username)).thenReturn(false);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);
        when(valueOperations.increment("auth:fail:" + username)).thenReturn(1L);

        AuthRequest request = new AuthRequest();
        request.setUsername(username);
        request.setPassword("bad");

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("用户名或密码错误");

        verify(valueOperations).increment("auth:fail:" + username);
        verify(redisTemplate).expire("auth:fail:" + username, 15, TimeUnit.MINUTES);
        verify(valueOperations, never()).set(eq("auth:lock:" + username), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void loginFailureShouldTriggerLockWhenThresholdReached() {
        String username = "throttle";
        User user = new User();
        user.setId(2L);
        user.setUsername(username);
        user.setPassword("hash");
        user.setRole("STUDENT");
        user.setNickname("Throttle");
        user.setCreatedAt(LocalDateTime.now());

        when(redisTemplate.hasKey("auth:lock:" + username)).thenReturn(false);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
        when(valueOperations.increment("auth:fail:" + username)).thenReturn(5L);

        AuthRequest request = new AuthRequest();
        request.setUsername(username);
        request.setPassword("wrong");

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(valueOperations).increment("auth:fail:" + username);
        verify(valueOperations).set("auth:lock:" + username, "1", 15, TimeUnit.MINUTES);
    }
}
