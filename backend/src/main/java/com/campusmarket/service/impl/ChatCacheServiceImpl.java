package com.campusmarket.service.impl;

import com.campusmarket.service.ChatCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class ChatCacheServiceImpl implements ChatCacheService {

    private static final Logger log = LoggerFactory.getLogger(ChatCacheServiceImpl.class);

    public static final String UNREAD_KEY_PREFIX = "chat:unread:";
    private static final String PROCESSED_KEY_PREFIX = "chat:unread:processed:";
    private static final long TTL_SECONDS = 7 * 24 * 3600L;

    private final StringRedisTemplate redisTemplate;

    public ChatCacheServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void incrementUnread(Long receiverId, Long senderId, Long messageId) {
        if (redisTemplate == null || receiverId == null || senderId == null || messageId == null) {
            return;
        }
        try {
            String processedKey = processedKey(receiverId);
            Long added = redisTemplate.opsForSet()
                    .add(processedKey, messageId.toString());
            if (added == null || added == 0L) {
                return;
            }
            redisTemplate.expire(processedKey, TTL_SECONDS, TimeUnit.SECONDS);
            String unreadKey = unreadKey(receiverId);
            redisTemplate.opsForHash().increment(unreadKey, senderId.toString(), 1L);
            redisTemplate.expire(unreadKey, TTL_SECONDS, TimeUnit.SECONDS);
        } catch (RuntimeException ex) {
            log.debug("Failed to increment unread counter receiver={}, sender={}, message={}",
                    receiverId, senderId, messageId, ex);
        }
    }

    @Override
    public void clearUnread(Long userId, Long partnerId) {
        if (redisTemplate == null || userId == null || partnerId == null) {
            return;
        }
        try {
            redisTemplate.opsForHash().delete(unreadKey(userId), partnerId.toString());
        } catch (RuntimeException ex) {
            log.debug("Failed to clear unread counter user={}, partner={}", userId, partnerId, ex);
        }
    }

    @Override
    public Map<Long, Integer> getUnreadCounts(Long userId) {
        if (redisTemplate == null || userId == null) {
            return Collections.emptyMap();
        }
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(unreadKey(userId));
            if (CollectionUtils.isEmpty(entries)) {
                return Collections.emptyMap();
            }
            Map<Long, Integer> result = new HashMap<>();
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                Long partnerId = parseLong(entry.getKey());
                Integer count = parseInt(entry.getValue());
                if (partnerId != null && count != null) {
                    result.put(partnerId, count);
                }
            }
            return result;
        } catch (RuntimeException ex) {
            log.debug("Failed to read unread counters user={}", userId, ex);
            return Collections.emptyMap();
        }
    }

    private String unreadKey(Long userId) {
        return UNREAD_KEY_PREFIX + userId;
    }

    private String processedKey(Long userId) {
        return PROCESSED_KEY_PREFIX + userId;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(Objects.toString(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseInt(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(Objects.toString(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
