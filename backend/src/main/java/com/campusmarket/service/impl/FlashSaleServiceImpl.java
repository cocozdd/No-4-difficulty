package com.campusmarket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusmarket.dto.FlashSaleItemCreateRequest;
import com.campusmarket.dto.FlashSaleItemResponse;
import com.campusmarket.entity.FlashSaleItem;
import com.campusmarket.entity.FlashSaleOrder;
import com.campusmarket.mapper.FlashSaleItemMapper;
import com.campusmarket.mapper.FlashSaleOrderMapper;
import com.campusmarket.service.FlashSaleService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FlashSaleServiceImpl implements FlashSaleService {

    private static final Logger log = LoggerFactory.getLogger(FlashSaleServiceImpl.class);

    private static final String STOCK_KEY_PREFIX = "flash:stock:";
    private static final String ITEM_CACHE_PREFIX = "flash:item:";
    private static final String USER_LOCK_PREFIX = "flash:user:";

    private final FlashSaleItemMapper flashSaleItemMapper;
    private final FlashSaleOrderMapper flashSaleOrderMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public FlashSaleServiceImpl(FlashSaleItemMapper flashSaleItemMapper,
                                FlashSaleOrderMapper flashSaleOrderMapper,
                                StringRedisTemplate redisTemplate,
                                ObjectMapper objectMapper) {
        this.flashSaleItemMapper = flashSaleItemMapper;
        this.flashSaleOrderMapper = flashSaleOrderMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public FlashSaleItemResponse createFlashSaleItem(FlashSaleItemCreateRequest request) {
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
        FlashSaleItem item = new FlashSaleItem();
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setOriginalPrice(request.getOriginalPrice());
        item.setFlashPrice(request.getFlashPrice());
        item.setTotalStock(request.getTotalStock());
        item.setStartTime(request.getStartTime());
        item.setEndTime(request.getEndTime());
        item.setStatus("SCHEDULED");
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        flashSaleItemMapper.insert(item);

        cacheItem(item);
        cacheStock(item.getId(), request.getTotalStock(), Duration.between(LocalDateTime.now(), request.getEndTime()).plusHours(1));

        return toResponse(item, request.getTotalStock());
    }

    @Override
    public List<FlashSaleItemResponse> listUpcomingAndActiveItems() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<FlashSaleItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(FlashSaleItem::getEndTime, now.minusSeconds(1))
                .orderByAsc(FlashSaleItem::getStartTime);
        List<FlashSaleItem> items = flashSaleItemMapper.selectList(wrapper);
        List<FlashSaleItemResponse> responses = new ArrayList<>();
        for (FlashSaleItem item : items) {
            int remaining = getRemainingStock(item.getId(), item.getTotalStock());
            responses.add(toResponse(item, remaining));
        }
        return responses;
    }

    @Override
    @Transactional
    public Long attemptPurchase(Long userId, Long flashSaleItemId) {
        if (userId == null || flashSaleItemId == null) {
            throw new IllegalArgumentException("用户ID与秒杀活动ID不能为空");
        }
        FlashSaleItem item = findFlashSaleItem(flashSaleItemId)
                .orElseThrow(() -> new IllegalArgumentException("秒杀活动不存在"));

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(item.getStartTime())) {
            throw new IllegalStateException("秒杀尚未开始");
        }
        if (now.isAfter(item.getEndTime())) {
            throw new IllegalStateException("秒杀已结束");
        }

        Duration expire = Duration.between(now, item.getEndTime()).plusMinutes(5);
        if (expire.isNegative()) {
            expire = Duration.ofMinutes(5);
        }

        String userKey = USER_LOCK_PREFIX + flashSaleItemId + ":" + userId;
        Boolean lockSuccess = redisTemplate.opsForValue().setIfAbsent(userKey, "1", expire);
        if (lockSuccess == null) {
            throw new IllegalStateException("系统繁忙，请稍后再试");
        }
        if (!lockSuccess) {
            throw new IllegalStateException("已经抢购成功，请勿重复请求");
        }

        String stockKey = STOCK_KEY_PREFIX + flashSaleItemId;
        Long stockLeft = redisTemplate.opsForValue().decrement(stockKey);
        if (stockLeft == null) {
            redisTemplate.delete(userKey);
            throw new IllegalStateException("库存信息异常");
        }
        if (stockLeft < 0) {
            redisTemplate.opsForValue().increment(stockKey);
            redisTemplate.delete(userKey);
            throw new IllegalStateException("秒杀已售罄");
        }

        FlashSaleOrder order = new FlashSaleOrder();
        order.setFlashSaleItemId(flashSaleItemId);
        order.setUserId(userId);
        order.setStatus("PREPARING");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        flashSaleOrderMapper.insert(order);

        return order.getId();
    }

    private Optional<FlashSaleItem> findFlashSaleItem(Long id) {
        FlashSaleItem cached = getItemFromCache(id);
        if (cached != null) {
            return Optional.of(cached);
        }
        FlashSaleItem dbItem = flashSaleItemMapper.selectById(id);
        if (dbItem != null) {
            cacheItem(dbItem);
        }
        return Optional.ofNullable(dbItem);
    }

    private void cacheItem(FlashSaleItem item) {
        try {
            String payload = objectMapper.writeValueAsString(item);
            Duration ttl = Duration.between(LocalDateTime.now(), item.getEndTime()).plusHours(6);
            if (ttl.isNegative()) {
                ttl = Duration.ofHours(6);
            }
            redisTemplate.opsForValue().set(ITEM_CACHE_PREFIX + item.getId(), payload, ttl);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to cache flash sale item {}: {}", item.getId(), ex.getMessage());
        }
    }

    private void cacheStock(Long itemId, Integer stock, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(STOCK_KEY_PREFIX + itemId, String.valueOf(stock), ttl);
        } catch (RuntimeException ex) {
            log.warn("Failed to cache flash sale stock itemId={}", itemId, ex);
        }
    }

    private FlashSaleItem getItemFromCache(Long id) {
        try {
            String json = redisTemplate.opsForValue().get(ITEM_CACHE_PREFIX + id);
            if (!StringUtils.hasText(json)) {
                return null;
            }
            return objectMapper.readValue(json, FlashSaleItem.class);
        } catch (Exception ex) {
            log.warn("Failed to read flash sale item from cache id={}", id, ex);
            return null;
        }
    }

    private int getRemainingStock(Long itemId, Integer defaultStock) {
        try {
            String value = redisTemplate.opsForValue().get(STOCK_KEY_PREFIX + itemId);
            if (value == null) {
                return defaultStock == null ? 0 : defaultStock;
            }
            return Integer.parseInt(value);
        } catch (RuntimeException ex) {
            log.warn("Failed to read remaining stock for itemId={}", itemId, ex);
            return defaultStock == null ? 0 : defaultStock;
        }
    }

    private FlashSaleItemResponse toResponse(FlashSaleItem item, Integer remainingStock) {
        LocalDateTime now = LocalDateTime.now();
        String status;
        if (now.isBefore(item.getStartTime())) {
            status = "SCHEDULED";
        } else if (now.isAfter(item.getEndTime())) {
            status = "ENDED";
        } else {
            status = "RUNNING";
        }
        return new FlashSaleItemResponse(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getOriginalPrice(),
                item.getFlashPrice(),
                item.getTotalStock(),
                remainingStock,
                item.getStartTime(),
                item.getEndTime(),
                status
        );
    }
}
