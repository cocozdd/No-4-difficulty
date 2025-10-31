package com.campusmarket.service.impl;

import com.campusmarket.service.LoginSessionService;
import com.campusmarket.service.SessionAccessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class LoginSessionServiceImpl implements LoginSessionService {

    private static final Logger log = LoggerFactory.getLogger(LoginSessionServiceImpl.class);

    private static final String SESSION_KEY_PREFIX = "auth:session:";
    private static final String USER_SESSIONS_PREFIX = "auth:user-sessions:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public LoginSessionServiceImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void storeSession(String token,
                             Long userId,
                             String username,
                             String role,
                             long ttlMillis,
                             String ip,
                             String userAgent) {
        if (!StringUtils.hasText(token) || userId == null) {
            return;
        }
        long issuedAt = Instant.now().toEpochMilli();
        long expiresAt = issuedAt + ttlMillis;
        LoginSession session = new LoginSession(
                userId,
                username,
                role,
                issuedAt,
                expiresAt,
                issuedAt,
                ip,
                userAgent
        );
        try {
            String payload = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue()
                    .set(sessionKey(token), payload, ttlMillis, TimeUnit.MILLISECONDS);
            redisTemplate.opsForSet()
                    .add(userSessionsKey(userId), token);
            redisTemplate.expire(userSessionsKey(userId), ttlMillis, TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to store login session for userId={}, reason={}", userId, ex.getMessage());
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable when storing session userId={}", userId, ex);
        }
    }

    @Override
    public void revokeSession(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        try {
            Optional<LoginSession> sessionOptional = getSession(token);
            sessionOptional.ifPresent(session ->
                    redisTemplate.opsForSet().remove(userSessionsKey(session.getUserId()), token));
            redisTemplate.delete(sessionKey(token));
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable when revoking session token={}", token, ex);
        }
    }

    @Override
    public void revokeAllSessions(Long userId) {
        if (userId == null) {
            return;
        }
        String userKey = userSessionsKey(userId);
        try {
            Set<String> tokens = redisTemplate.opsForSet().members(userKey);
            if (tokens != null) {
                for (String token : tokens) {
                    redisTemplate.delete(sessionKey(token));
                }
            }
            redisTemplate.delete(userKey);
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable when revoking all sessions for userId={}", userId, ex);
        }
    }

    @Override
    public Optional<LoginSession> getSession(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        try {
            String payload = redisTemplate.opsForValue().get(sessionKey(token));
            if (!StringUtils.hasText(payload)) {
                return Optional.empty();
            }
            LoginSession session = objectMapper.readValue(payload, LoginSession.class);
            return Optional.of(session);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to deserialize login session token={}, payload corrupted", token);
            redisTemplate.delete(sessionKey(token));
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable when fetching session token={}", token, ex);
            throw new SessionAccessException("Session store unavailable", ex);
        }
    }

    @Override
    public void refreshSession(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        try {
            Optional<LoginSession> sessionOptional;
            try {
                sessionOptional = getSession(token);
            } catch (SessionAccessException ex) {
                return;
            }
            if (sessionOptional.isEmpty()) {
                return;
            }
            LoginSession session = sessionOptional.get();
            session.setLastSeenAt(Instant.now().toEpochMilli());
            String payload = objectMapper.writeValueAsString(session);

            Long ttl = redisTemplate.getExpire(sessionKey(token), TimeUnit.MILLISECONDS);
            long remaining = ttl == null ? 0L : ttl;
            if (remaining <= 0) {
                return;
            }
            redisTemplate.opsForValue()
                    .set(sessionKey(token), payload, remaining, TimeUnit.MILLISECONDS);
            redisTemplate.expire(userSessionsKey(session.getUserId()), remaining, TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to refresh login session token={}", token, ex);
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable when refreshing session token={}", token, ex);
        }
    }

    private String sessionKey(String token) {
        return SESSION_KEY_PREFIX + token;
    }

    private String userSessionsKey(Long userId) {
        return USER_SESSIONS_PREFIX + userId;
    }
}
