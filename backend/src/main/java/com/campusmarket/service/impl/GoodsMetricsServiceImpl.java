package com.campusmarket.service.impl;

import com.campusmarket.service.GoodsMetricsService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoodsMetricsServiceImpl implements GoodsMetricsService {

    private static final String KEY_ORDERS = "goods:metrics:orders";
    private static final String KEY_CARTS = "goods:metrics:carts";
    private static final String KEY_VIEWS = "goods:metrics:views";
    private static final String ZSET_ORDERS = "goods:ranking:orders";
    private static final String ZSET_CARTS = "goods:ranking:carts";
    private static final String ZSET_VIEWS = "goods:ranking:views";

    private final StringRedisTemplate redisTemplate;

    public GoodsMetricsServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void recordView(Long goodsId) {
        increment(KEY_VIEWS, goodsId);
        zincrement(ZSET_VIEWS, goodsId);
    }

    @Override
    public void recordOrder(Long goodsId) {
        increment(KEY_ORDERS, goodsId);
        zincrement(ZSET_ORDERS, goodsId);
    }

    @Override
    public void recordCartAddition(Long goodsId) {
        increment(KEY_CARTS, goodsId);
        zincrement(ZSET_CARTS, goodsId);
    }

    @Override
    public Map<Long, Long> getOrderCounts(Collection<Long> goodsIds) {
        return getCounts(KEY_ORDERS, goodsIds);
    }

    @Override
    public Map<Long, Long> getCartCounts(Collection<Long> goodsIds) {
        return getCounts(KEY_CARTS, goodsIds);
    }

    @Override
    public Map<Long, Long> getViewCounts(Collection<Long> goodsIds) {
        return getCounts(KEY_VIEWS, goodsIds);
    }

    @Override
    public void removeMetrics(Long goodsId) {
        if (goodsId == null) {
            return;
        }
        try {
            String field = goodsId.toString();
            redisTemplate.opsForHash().delete(KEY_VIEWS, field);
            redisTemplate.opsForHash().delete(KEY_CARTS, field);
            redisTemplate.opsForHash().delete(KEY_ORDERS, field);
            redisTemplate.opsForZSet().remove(ZSET_VIEWS, field);
            redisTemplate.opsForZSet().remove(ZSET_CARTS, field);
            redisTemplate.opsForZSet().remove(ZSET_ORDERS, field);
        } catch (RuntimeException ignored) {
        }
    }

    private void increment(String key, Long goodsId) {
        if (goodsId == null) {
            return;
        }
        try {
            redisTemplate.opsForHash().increment(key, goodsId.toString(), 1);
        } catch (RuntimeException ignored) {
        }
    }

    private Map<Long, Long> getCounts(String key, Collection<Long> goodsIds) {
        Map<Long, Long> result = new HashMap<>();
        if (CollectionUtils.isEmpty(goodsIds)) {
            return result;
        }
        List<Object> fields = goodsIds.stream().map(String::valueOf).collect(Collectors.toList());
        try {
            List<Object> values = redisTemplate.opsForHash().multiGet(key, fields);
            for (int i = 0; i < fields.size(); i++) {
                Object field = fields.get(i);
                Object value = values.get(i);
                long count = value == null ? 0 : Long.parseLong(value.toString());
                result.put(Long.parseLong(field.toString()), count);
            }
        } catch (RuntimeException ignored) {
            goodsIds.forEach(id -> result.put(id, 0L));
        }
        return result;
    }

    private void zincrement(String key, Long goodsId) {
        if (goodsId == null) {
            return;
        }
        try {
            redisTemplate.opsForZSet().incrementScore(key, goodsId.toString(), 1D);
        } catch (RuntimeException ignored) {
        }
    }
}
