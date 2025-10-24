package com.campusmarket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusmarket.dto.GoodsFilterRequest;
import com.campusmarket.dto.GoodsResponse;
import com.campusmarket.dto.GoodsUpdateRequest;
import com.campusmarket.entity.Goods;
import com.campusmarket.entity.GoodsStatus;
import com.campusmarket.mapper.GoodsMapper;
import com.campusmarket.messaging.GoodsEventPublisher;
import com.campusmarket.service.CartService;
import com.campusmarket.service.GoodsCacheService;
import com.campusmarket.service.GoodsMetricsService;
import com.campusmarket.service.GoodsService;
import com.campusmarket.service.HotGoodsService;
import com.campusmarket.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.campusmarket.service.impl.GoodsCacheServiceImpl.GOODS_DETAIL_KEY_PREFIX;
import static com.campusmarket.service.impl.GoodsCacheServiceImpl.GOODS_LIST_KEY_PREFIX;

@Service
public class GoodsServiceImpl implements GoodsService {

    private static final String NULL_MARKER = "__NULL__";
    private static final int DETAIL_TTL_SECONDS = 600;
    private static final int LIST_TTL_SECONDS = 180;
    private static final int NULL_TTL_SECONDS = 30;
    private static final int TTL_JITTER_SECONDS = 60;

    private final GoodsMapper goodsMapper;
    private final UserService userService;
    private final CartService cartService;
    private final HotGoodsService hotGoodsService;
    private final GoodsMetricsService goodsMetricsService;
    private final GoodsCacheService goodsCacheService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final GoodsEventPublisher goodsEventPublisher;

    public GoodsServiceImpl(GoodsMapper goodsMapper,
                            UserService userService,
                            CartService cartService,
                            HotGoodsService hotGoodsService,
                            GoodsMetricsService goodsMetricsService,
                            GoodsCacheService goodsCacheService,
                            RedisTemplate<String, Object> redisTemplate,
                            ObjectMapper objectMapper,
                            GoodsEventPublisher goodsEventPublisher) {
        this.goodsMapper = goodsMapper;
        this.userService = userService;
        this.cartService = cartService;
        this.hotGoodsService = hotGoodsService;
        this.goodsMetricsService = goodsMetricsService;
        this.goodsCacheService = goodsCacheService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.goodsEventPublisher = goodsEventPublisher;
    }

    @PostConstruct
    @Override
    public void seedInitialGoods() {
        if (goodsMapper.selectCount(null) > 0) {
            return;
        }
        Goods laptop = new Goods();
        laptop.setTitle("Second-hand Laptop");
        laptop.setDescription("95% new ultrabook, perfect for course work");
        laptop.setCategory("Electronics");
        laptop.setPrice(new BigDecimal("3200.00"));
        laptop.setCoverImageUrl("https://dummyimage.com/600x360/1e90ff/ffffff.png&text=Laptop");
        laptop.setSellerId(1L);
        laptop.setPublishedAt(LocalDateTime.now().minusDays(1));
        laptop.setQuantity(5);
        laptop.setSold(false);
        laptop.setDeleted(false);
        laptop.setStatus(GoodsStatus.APPROVED.name());

        Goods textbook = new Goods();
        textbook.setTitle("Linear Algebra Textbook");
        textbook.setDescription("No notes, no damage");
        textbook.setCategory("Books");
        textbook.setPrice(new BigDecimal("25.00"));
        textbook.setCoverImageUrl("https://dummyimage.com/600x360/34d399/ffffff.png&text=Book");
        textbook.setSellerId(1L);
        textbook.setPublishedAt(LocalDateTime.now().minusHours(10));
        textbook.setQuantity(10);
        textbook.setSold(false);
        textbook.setDeleted(false);
        textbook.setStatus(GoodsStatus.APPROVED.name());

        Goods bike = new Goods();
        bike.setTitle("Campus Bicycle");
        bike.setDescription("Well maintained, lock included");
        bike.setCategory("Daily");
        bike.setPrice(new BigDecimal("280.00"));
        bike.setCoverImageUrl("https://dummyimage.com/600x360/f97316/ffffff.png&text=Bike");
        bike.setSellerId(1L);
        bike.setPublishedAt(LocalDateTime.now().minusHours(3));
        bike.setQuantity(2);
        bike.setSold(false);
        bike.setDeleted(false);
        bike.setStatus(GoodsStatus.APPROVED.name());

        goodsMapper.insert(laptop);
        goodsMapper.insert(textbook);
        goodsMapper.insert(bike);
        hotGoodsService.evictHotCache();
    }

    @Override
    public List<GoodsResponse> listGoods(GoodsFilterRequest request) {
        String cacheKey = buildListCacheKey(request);
        List<GoodsResponse> cached = getCachedGoodsList(cacheKey);
        if (cached != null) {
            return cached;
        }

        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Goods::getSold, false);
        wrapper.eq(Goods::getDeleted, false);
        wrapper.eq(Goods::getStatus, GoodsStatus.APPROVED.name());
        wrapper.gt(Goods::getQuantity, 0);
        if (request != null) {
            if (StringUtils.hasText(request.getCategory())) {
                wrapper.eq(Goods::getCategory, request.getCategory());
            }
            if (request.getMinPrice() != null) {
                wrapper.ge(Goods::getPrice, request.getMinPrice());
            }
            if (request.getMaxPrice() != null) {
                wrapper.le(Goods::getPrice, request.getMaxPrice());
            }
            if (StringUtils.hasText(request.getKeyword())) {
                wrapper.like(Goods::getTitle, request.getKeyword());
            }
        }
        wrapper.orderByDesc(Goods::getPublishedAt);
        List<GoodsResponse> result = goodsMapper.selectList(wrapper)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        cacheGoodsList(cacheKey, result);
        return result;
    }

    @Override
    public GoodsResponse getGoods(Long id, Long viewerId, boolean adminView) {
        GoodsResponse cached = getCachedGoodsDetail(id);
        if (cached != null) {
            validateAccess(cached, viewerId, adminView);
            return cached;
        }

        Goods goods = getGoodsEntity(id);
        if (Boolean.TRUE.equals(goods.getDeleted())) {
            cacheGoodsNotFound(id);
            throw new IllegalArgumentException("Goods not found");
        }
        GoodsResponse response = toResponse(goods);
        validateAccess(response, viewerId, adminView);
        cacheGoodsDetail(response);
        return response;
    }

    @Override
    public Goods getGoodsEntity(Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null) {
            cacheGoodsNotFound(id);
            throw new IllegalArgumentException("Goods not found");
        }
        if (goods.getQuantity() == null) {
            goods.setQuantity(0);
        }
        if (!StringUtils.hasText(goods.getStatus())) {
            goods.setStatus(GoodsStatus.APPROVED.name());
        }
        return goods;
    }

    @Override
    public Goods createGoods(Goods goods) {
        Integer quantity = goods.getQuantity();
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        goods.setPublishedAt(LocalDateTime.now());
        goods.setSold(quantity <= 0);
        goods.setDeleted(false);
        goods.setStatus(GoodsStatus.PENDING_REVIEW.name());
        if (!StringUtils.hasText(goods.getCoverImageUrl())) {
            goods.setCoverImageUrl(getFallbackImage(goods.getCategory()));
        }
        goodsMapper.insert(goods);
        evictCachesForGoods(goods.getId());
        goodsEventPublisher.publishGoodsCreated(goods);
        return goods;
    }

    @Override
    public GoodsResponse updateGoods(Long id, GoodsUpdateRequest request, Long sellerId) {
        Goods goods = getGoodsEntity(id);
        if (goods == null || Boolean.TRUE.equals(goods.getDeleted())) {
            throw new IllegalArgumentException("Goods not found");
        }
        if (!goods.getSellerId().equals(sellerId)) {
            throw new AccessDeniedException("无权限操作该商品");
        }
        if (Boolean.TRUE.equals(goods.getSold())) {
            throw new IllegalArgumentException("商品已售出，无法修改");
        }
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        goods.setDescription(request.getDescription());
        goods.setPrice(request.getPrice());
        if (StringUtils.hasText(request.getCoverImageUrl())) {
            goods.setCoverImageUrl(request.getCoverImageUrl());
        }
        goods.setQuantity(request.getQuantity());
        goods.setSold(goods.getQuantity() <= 0);
        goods.setStatus(GoodsStatus.PENDING_REVIEW.name());
        goodsMapper.updateById(goods);
        evictCachesForGoods(goods.getId());
        goodsEventPublisher.publishGoodsUpdated(goods, sellerId, "Seller updated goods and reset status to pending review");
        return toResponse(goods);
    }

    @Override
    public void deleteGoods(Long id, Long sellerId) {
        Goods goods = getGoodsEntity(id);
        if (goods == null || Boolean.TRUE.equals(goods.getDeleted())) {
            throw new IllegalArgumentException("Goods not found");
        }
        if (!goods.getSellerId().equals(sellerId)) {
            throw new AccessDeniedException("无权限操作该商品");
        }
        if (Boolean.TRUE.equals(goods.getSold())) {
            throw new IllegalArgumentException("商品已售出，无法删除");
        }
        goods.setDeleted(true);
        goodsMapper.updateById(goods);
        evictCachesForGoods(id);
        goodsMetricsService.removeMetrics(id);
        goodsEventPublisher.publishGoodsDeleted(goods, sellerId);
    }

    @Override
    public List<GoodsResponse> listGoodsBySeller(Long sellerId) {
        return goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                        .eq(Goods::getSellerId, sellerId)
                        .eq(Goods::getDeleted, false)
                        .orderByDesc(Goods::getPublishedAt))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<GoodsResponse> listGoodsByStatus(GoodsStatus status) {
        return goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                        .eq(Goods::getStatus, status.name())
                        .eq(Goods::getDeleted, false)
                        .orderByDesc(Goods::getPublishedAt))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public GoodsResponse reviewGoods(Long id, GoodsStatus status) {
        Goods goods = getGoodsEntity(id);
        if (Boolean.TRUE.equals(goods.getDeleted())) {
            cacheGoodsNotFound(id);
            throw new IllegalArgumentException("Goods not found");
        }
        if (GoodsStatus.APPROVED.equals(status)
                && (Boolean.TRUE.equals(goods.getSold())
                || goods.getQuantity() == null
                || goods.getQuantity() <= 0)) {
            throw new IllegalArgumentException("Not enough stock to approve the listing");
        }
        goods.setStatus(status.name());
        if (GoodsStatus.APPROVED.equals(status)) {
            goods.setPublishedAt(LocalDateTime.now());
        }
        goods.setSold(goods.getQuantity() != null && goods.getQuantity() <= 0);
        goodsMapper.updateById(goods);
        evictCachesForGoods(id);
        goodsEventPublisher.publishGoodsReviewed(goods, null, "Status updated to " + status.name());
        return toResponse(goods);
    }

    @Override
    public void markSold(Long goodsId, boolean soldOperation) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            cacheGoodsNotFound(goodsId);
            throw new IllegalArgumentException("Goods not found");
        }
        int quantity = goods.getQuantity() == null ? 0 : goods.getQuantity();
        if (soldOperation) {
            if (quantity <= 0) {
                throw new IllegalStateException("Insufficient stock for this item");
            }
            goods.setQuantity(quantity - 1);
        } else {
            goods.setQuantity(quantity + 1);
        }
        goods.setSold(goods.getQuantity() <= 0);
        goodsMapper.updateById(goods);
        if (Boolean.TRUE.equals(goods.getSold()) || goods.getQuantity() == null || goods.getQuantity() <= 0) {
            cartService.removeCartItemsByGoodsId(goodsId);
        }
        evictCachesForGoods(goodsId);
        goodsEventPublisher.publishGoodsMarkedSold(goods, null);
    }

    private GoodsResponse toResponse(Goods goods) {
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

    private String getFallbackImage(String category) {
        if ("Electronics".equalsIgnoreCase(category)) {
            return "https://dummyimage.com/600x360/1e90ff/ffffff.png&text=Electronics";
        }
        if ("Books".equalsIgnoreCase(category)) {
            return "https://dummyimage.com/600x360/34d399/ffffff.png&text=Books";
        }
        if ("Daily".equalsIgnoreCase(category)) {
            return "https://dummyimage.com/600x360/f97316/ffffff.png&text=Daily";
        }
        return "https://dummyimage.com/600x360/f97316/ffffff.png&text=Goods";
    }

    private void cacheGoodsDetail(GoodsResponse goods) {
        redisTemplate.opsForValue()
                .set(GOODS_DETAIL_KEY_PREFIX + goods.getId(), goods, randomizeTtl(DETAIL_TTL_SECONDS));
    }

    private void cacheGoodsNotFound(Long goodsId) {
        redisTemplate.opsForValue()
                .set(GOODS_DETAIL_KEY_PREFIX + goodsId, NULL_MARKER, Duration.ofSeconds(NULL_TTL_SECONDS));
    }

    private GoodsResponse getCachedGoodsDetail(Long goodsId) {
        Object cached = redisTemplate.opsForValue().get(GOODS_DETAIL_KEY_PREFIX + goodsId);
        if (cached == null) {
            return null;
        }
        if (cached instanceof String && NULL_MARKER.equals(cached)) {
            throw new IllegalArgumentException("Goods not found");
        }
        return convertValue(cached, GoodsResponse.class);
    }

    private void cacheGoodsList(String key, List<GoodsResponse> data) {
        redisTemplate.opsForValue().set(key, data, randomizeTtl(LIST_TTL_SECONDS));
    }

    private List<GoodsResponse> getCachedGoodsList(String key) {
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            return null;
        }
        if (cached instanceof List<?>) {
            List<?> list = (List<?>) cached;
            if (list.isEmpty() || list.get(0) instanceof GoodsResponse) {
                @SuppressWarnings("unchecked")
                List<GoodsResponse> typed = (List<GoodsResponse>) list;
                return typed;
            }
        }
        return objectMapper.convertValue(cached, new TypeReference<List<GoodsResponse>>() {});
    }

    private String buildListCacheKey(GoodsFilterRequest request) {
        if (request == null) {
            return GOODS_LIST_KEY_PREFIX + "all";
        }
        String raw = String.format("category=%s|minPrice=%s|maxPrice=%s|keyword=%s",
                defaultString(request.getCategory()),
                Objects.toString(request.getMinPrice(), ""),
                Objects.toString(request.getMaxPrice(), ""),
                defaultString(request.getKeyword()));
        String digest = DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
        return GOODS_LIST_KEY_PREFIX + digest;
    }

    private Duration randomizeTtl(int baseSeconds) {
        int jitter = ThreadLocalRandom.current().nextInt(TTL_JITTER_SECONDS + 1);
        return Duration.ofSeconds(baseSeconds + jitter);
    }

    private void evictCachesForGoods(Long goodsId) {
        goodsCacheService.evictAllForGoods(goodsId);
        hotGoodsService.evictHotCache();
    }

    private void validateAccess(GoodsResponse goods, Long viewerId, boolean adminView) {
        boolean isOwner = viewerId != null && viewerId.equals(goods.getSellerId());
        if (!adminView && !isOwner && !GoodsStatus.APPROVED.name().equals(goods.getStatus())) {
            throw new AccessDeniedException("无权限查看该商品");
        }
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private <T> T convertValue(Object source, Class<T> targetType) {
        if (targetType.isInstance(source)) {
            return targetType.cast(source);
        }
        return objectMapper.convertValue(source, targetType);
    }
}
