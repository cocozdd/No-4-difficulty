package com.campusmarket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusmarket.dto.AuthRequest;
import com.campusmarket.dto.AuthResponse;
import com.campusmarket.dto.RegisterRequest;
import com.campusmarket.dto.UserProfileResponse;
import com.campusmarket.entity.User;
import com.campusmarket.mapper.UserMapper;
import com.campusmarket.security.jwt.JwtTokenProvider;
import com.campusmarket.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String LOGIN_FAIL_PREFIX = "auth:fail:";
    private static final String LOGIN_LOCK_PREFIX = "auth:lock:";
    private static final int FAIL_THRESHOLD = 5;
    private static final long FAIL_WINDOW_MINUTES = 15;
    private static final long LOCK_DURATION_MINUTES = 15;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    public UserServiceImpl(UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           StringRedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void seedAdmin() {
        Optional.ofNullable(userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "admin")))
                .orElseGet(() -> {
                    User admin = new User();
                    admin.setUsername("admin");
                    admin.setPassword(passwordEncoder.encode("admin123"));
                    admin.setRole("ADMIN");
                    admin.setNickname("管理员");
                    admin.setCreatedAt(LocalDateTime.now());
                    admin.setPhone("13800000000");
                    userMapper.insert(admin);
                    return admin;
                });
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("STUDENT");
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
        Long userId = user.getId();
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new AuthResponse(userId, user.getUsername(), token, user.getRole(), user.getNickname(),
                jwtTokenProvider.getExpirationMs());
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        String username = request.getUsername();
        if (isLocked(username)) {
            throw new BadCredentialsException("登录失败次数过多，请稍后再试");
        }
        User user = findByUsername(request.getUsername());
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordFailure(username);
            throw new BadCredentialsException("用户名或密码错误");
        }
        clearFailures(username);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new AuthResponse(user.getId(), user.getUsername(), token, user.getRole(), user.getNickname(),
                jwtTokenProvider.getExpirationMs());
    }

    @Override
    public UserProfileResponse getProfile(Long userId) {
        User user = findById(userId);
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getRole(),
                user.getAvatarUrl(),
                user.getPhone()
        );
    }

    @Override
    public User findById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }

    @Override
    public User findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }

    private boolean existsByUsername(String username) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)) > 0;
    }

    private boolean isLocked(String username) {
        if (redisTemplate == null || !StringUtils.hasText(username)) {
            return false;
        }
        try {
            String key = LOGIN_LOCK_PREFIX + username;
            Boolean locked = redisTemplate.hasKey(key);
            return locked != null && locked;
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable when checking login lock for {}", username, ex);
            return false;
        }
    }

    private void recordFailure(String username) {
        if (redisTemplate == null || !StringUtils.hasText(username)) {
            return;
        }
        try {
            String failKey = LOGIN_FAIL_PREFIX + username;
            Long failures = redisTemplate.opsForValue().increment(failKey);
            if (failures != null && failures == 1L) {
                redisTemplate.expire(failKey, FAIL_WINDOW_MINUTES, TimeUnit.MINUTES);
            }
            if (failures != null && failures >= FAIL_THRESHOLD) {
                String lockKey = LOGIN_LOCK_PREFIX + username;
                redisTemplate.opsForValue().set(lockKey, "1", LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
            }
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable when recording login failure for {}", username, ex);
        }
    }

    private void clearFailures(String username) {
        if (redisTemplate == null || !StringUtils.hasText(username)) {
            return;
        }
        try {
            redisTemplate.delete(LOGIN_FAIL_PREFIX + username);
            redisTemplate.delete(LOGIN_LOCK_PREFIX + username);
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable when clearing login failures for {}", username, ex);
        }
    }
}
