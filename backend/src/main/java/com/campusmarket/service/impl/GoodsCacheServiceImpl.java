package com.campusmarket.service.impl;

import com.campusmarket.service.GoodsCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Set;

@Service
public class GoodsCacheServiceImpl implements GoodsCacheService {

    private static final Logger log = LoggerFactory.getLogger(GoodsCacheServiceImpl.class);

    /**
     * Cache prefixes kept in sync with GoodsServiceImpl.
     */
    public static final String GOODS_DETAIL_KEY_PREFIX = "goods:detail:";
    public static final String GOODS_LIST_KEY_PREFIX = "goods:list:";

    private final RedisTemplate<String, Object> redisTemplate;

    public GoodsCacheServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void evictGoodsDetail(Long goodsId) {
        if (goodsId == null) {
            return;
        }
        String key = GOODS_DETAIL_KEY_PREFIX + goodsId;
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException ex) {
            log.debug("Failed to delete goods detail cache key={}", key, ex);
        }
    }

    @Override
    public void evictGoodsLists() {
        try {
            Set<String> keys = redisTemplate.keys(GOODS_LIST_KEY_PREFIX + "*");
            if (!CollectionUtils.isEmpty(keys)) {
                redisTemplate.delete(keys);
            }
        } catch (RuntimeException ex) {
            log.debug("Failed to delete goods list cache keys", ex);
        }
    }
}
