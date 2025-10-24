package com.campusmarket.service.impl;

import com.campusmarket.service.ChatMetricsService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ChatMetricsServiceImpl implements ChatMetricsService {

    private static final String KEY_SENT = "chat:metrics:sent";
    private static final String KEY_RECEIVED = "chat:metrics:received";
    private static final long TTL_SECONDS = 7 * 24 * 3600;

    private final StringRedisTemplate redisTemplate;

    public ChatMetricsServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void recordMessageCreated(Long senderId, Long receiverId) {
        if (redisTemplate == null) {
            return;
        }
        try {
            if (senderId != null) {
                incrementHash(KEY_SENT, senderId);
            }
            if (receiverId != null) {
                incrementHash(KEY_RECEIVED, receiverId);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void incrementHash(String key, Long userId) {
        Long value = redisTemplate.opsForHash().increment(key, userId.toString(), 1L);
        if (value != null && value == 1L) {
            redisTemplate.expire(key, TTL_SECONDS, TimeUnit.SECONDS);
        }
    }
}
