package com.campusmarket.service.impl;

import com.campusmarket.dto.GoodsResponse;
import com.campusmarket.dto.HotGoodsItemResponse;
import com.campusmarket.entity.Goods;
import com.campusmarket.entity.GoodsStatus;
import com.campusmarket.mapper.GoodsMapper;
import com.campusmarket.service.GoodsMetricsService;
import com.campusmarket.service.HotGoodsService;
import com.campusmarket.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class HotGoodsServiceImpl implements HotGoodsService {

    private static final String HOT_CACHE_KEY = "goods:hot:cache";
    private static final long HOT_CACHE_TTL_SECONDS = 300;
    private static final Map<String, String> RANKING_KEYS = Map.of(
            "orders", "goods:ranking:orders",
            "carts", "goods:ranking:carts",
            "views", "goods:ranking:views"
    );

    private final GoodsMapper goodsMapper;
    private final GoodsMetricsService metricsService;
    private final UserService userService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public HotGoodsServiceImpl(GoodsMapper goodsMapper,
                               GoodsMetricsService metricsService,
                               UserService userService,
                               StringRedisTemplate redisTemplate,
                               ObjectMapper objectMapper) {
        this.goodsMapper = goodsMapper;
        this.metricsService = metricsService;
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<HotGoodsItemResponse> getTopHotGoods(int limit) {
        List<HotGoodsItemResponse> cached = readCache();
        if (!CollectionUtils.isEmpty(cached)) {
            return cached.stream().limit(limit).collect(Collectors.toList());
        }

        List<Goods> goodsList = goodsMapper.selectList(null).stream()
                .filter(goods -> goods != null
                        && !Boolean.TRUE.equals(goods.getDeleted())
                        && GoodsStatus.APPROVED.name().equals(goods.getStatus())
                        && Boolean.FALSE.equals(goods.getSold())
                        && goods.getQuantity() != null
                        && goods.getQuantity() > 0)
                .collect(Collectors.toList());
        if (goodsList.isEmpty()) {
            return List.of();
        }

        List<Long> goodsIds = goodsList.stream().map(Goods::getId).collect(Collectors.toList());
        Map<Long, Long> orderCounts = metricsService.getOrderCounts(goodsIds);
        Map<Long, Long> cartCounts = metricsService.getCartCounts(goodsIds);
        Map<Long, Long> viewCounts = metricsService.getViewCounts(goodsIds);

        double maxOrder = maxValue(orderCounts);
        double maxCart = maxValue(cartCounts);
        double maxView = maxValue(viewCounts);

        LocalDateTime now = LocalDateTime.now();

        List<HotGoodsItemResponse> scored = new ArrayList<>();
        for (Goods goods : goodsList) {
            long orders = orderCounts.getOrDefault(goods.getId(), 0L);
            long carts = cartCounts.getOrDefault(goods.getId(), 0L);
            long views = viewCounts.getOrDefault(goods.getId(), 0L);

            double orderScore = maxOrder > 0 ? orders / maxOrder : 0;
            double cartScore = maxCart > 0 ? carts / maxCart : 0;
            double viewScore = maxView > 0 ? views / maxView : 0;
            double timeScore = computeTimeDecay(goods.getPublishedAt(), now);

            double score = orderScore * 0.4
                    + cartScore * 0.25
                    + viewScore * 0.25
                    + timeScore * 0.1;

            GoodsResponse goodsResponse = buildGoodsResponse(goods);

            scored.add(new HotGoodsItemResponse(goodsResponse, score));
        }

        scored.sort(Comparator.comparingDouble(HotGoodsItemResponse::getScore).reversed());
        List<HotGoodsItemResponse> top = scored.stream().limit(limit).collect(Collectors.toList());
        writeCache(scored);
        return top;
    }

    @Override
    public void evictHotCache() {
        try {
            redisTemplate.delete(HOT_CACHE_KEY);
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public List<HotGoodsItemResponse> getRanking(String metric, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        String normalized = metric == null ? "orders" : metric.toLowerCase();
        String key = RANKING_KEYS.get(normalized);
        if (key == null) {
            throw new IllegalArgumentException("Unsupported ranking metric: " + metric);
        }
        try {
            Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> tuples =
                    redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);
            if (CollectionUtils.isEmpty(tuples)) {
                return List.of();
            }
            List<Long> goodsIds = tuples.stream()
                    .map(tuple -> tuple.getValue() == null ? null : Long.parseLong(tuple.getValue()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (goodsIds.isEmpty()) {
                return List.of();
            }
            List<Goods> goodsList = goodsMapper.selectBatchIds(goodsIds);
            Map<Long, Goods> goodsMap = goodsList.stream()
                    .filter(g -> g != null
                            && !Boolean.TRUE.equals(g.getDeleted())
                            && GoodsStatus.APPROVED.name().equals(g.getStatus()))
                    .collect(Collectors.toMap(Goods::getId, g -> g));

            List<HotGoodsItemResponse> responses = new ArrayList<>();
            for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<String> tuple : tuples) {
                if (tuple.getValue() == null) {
                    continue;
                }
                long goodsId = Long.parseLong(tuple.getValue());
                Goods goods = goodsMap.get(goodsId);
                if (goods == null) {
                    continue;
                }
                GoodsResponse response = buildGoodsResponse(goods);
                double score = tuple.getScore() == null ? 0D : tuple.getScore();
                responses.add(new HotGoodsItemResponse(response, score));
            }
            return responses;
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private GoodsResponse buildGoodsResponse(Goods goods) {
        String nickname = userService.findById(goods.getSellerId()).getNickname();
        return new GoodsResponse(
                goods.getId(),
                goods.getTitle(),
                goods.getDescription(),
                goods.getCategory(),
                goods.getPrice(),
                goods.getCoverImageUrl(),
                goods.getPublishedAt(),
                goods.getSellerId(),
                nickname,
                Boolean.TRUE.equals(goods.getSold()),
                goods.getStatus(),
                goods.getQuantity()
        );
    }

    private double maxValue(Map<Long, Long> counts) {
        return counts.values().stream().mapToLong(Long::longValue).max().orElse(0);
    }

    private double computeTimeDecay(LocalDateTime publishedAt, LocalDateTime now) {
        if (publishedAt == null) {
            return 0.5;
        }
        long hours = Duration.between(publishedAt, now).toHours();
        double days = hours / 24.0;
        return Math.exp(-0.2 * days);
    }

    private List<HotGoodsItemResponse> readCache() {
        try {
            String cache = redisTemplate.opsForValue().get(HOT_CACHE_KEY);
            if (cache == null || cache.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(cache, new TypeReference<List<HotGoodsItemResponse>>() {});
        } catch (Exception ex) {
            try {
                redisTemplate.delete(HOT_CACHE_KEY);
            } catch (RuntimeException ignored) {
            }
            return List.of();
        }
    }

    private void writeCache(List<HotGoodsItemResponse> items) {
        try {
            String payload = objectMapper.writeValueAsString(items);
            redisTemplate.opsForValue().set(HOT_CACHE_KEY, payload, HOT_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }
}
