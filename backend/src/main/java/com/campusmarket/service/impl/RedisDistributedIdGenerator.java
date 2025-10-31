package com.campusmarket.service.impl;

import com.campusmarket.service.DistributedIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Redis backed implementation of a Snowflake-like ID generator.
 * <p>
 * Structure of generated 64-bit number:
 * <pre>
 * 0 - reserved sign bit
 * 41 bits - timestamp (milliseconds since {@link #CUSTOM_EPOCH})
 * 10 bits - shard placeholder (currently unused, set to 0)
 * 12 bits - per-millisecond sequence obtained from Redis INCR
 * </pre>
 */
@Component
public class RedisDistributedIdGenerator implements DistributedIdGenerator {

    private static final Logger log = LoggerFactory.getLogger(RedisDistributedIdGenerator.class);

    private static final long CUSTOM_EPOCH = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli();
    private static final int SEQUENCE_BITS = 12;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;
    private static final String KEY_PREFIX = "idgen:";

    private final StringRedisTemplate redisTemplate;

    public RedisDistributedIdGenerator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public long nextId(String businessTag) {
        String tag = normalize(businessTag);
        long currentMillis = System.currentTimeMillis();
        long timestampPart = (currentMillis - CUSTOM_EPOCH);
        if (timestampPart < 0) {
            timestampPart = 0;
        }

        String redisKey = KEY_PREFIX + tag + ":" + currentMillis;
        Long sequence = null;
        try {
            sequence = redisTemplate.opsForValue().increment(redisKey);
            if (sequence != null && sequence == 1L) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(2));
            }
        } catch (DataAccessException | IllegalStateException ex) {
            log.warn("Redis unavailable while generating id for tag={}, fallback to local generator", tag, ex);
        }

        if (sequence == null) {
            return fallbackId(currentMillis);
        }

        long seq = sequence & SEQUENCE_MASK;
        if (sequence > SEQUENCE_MASK) {
            // Busy millisecond, wait for the next millisecond and retry once
            try {
                TimeUnit.MILLISECONDS.sleep(1);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            return nextId(tag);
        }

        long id = (timestampPart << (SEQUENCE_BITS)) | seq;
        // Avoid negative numbers due to overflow
        return id & Long.MAX_VALUE;
    }

    private String normalize(String businessTag) {
        if (!StringUtils.hasText(businessTag)) {
            return "default";
        }
        return businessTag.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "-");
    }

    private long fallbackId(long currentMillis) {
        long random = ThreadLocalRandom.current().nextInt(1 << SEQUENCE_BITS);
        long id = ((currentMillis - CUSTOM_EPOCH) << SEQUENCE_BITS) | random;
        return id & Long.MAX_VALUE;
    }
}
