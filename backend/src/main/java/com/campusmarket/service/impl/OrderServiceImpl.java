package com.campusmarket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusmarket.dto.GoodsResponse;
import com.campusmarket.dto.OrderCreateRequest;
import com.campusmarket.dto.OrderResponse;
import com.campusmarket.dto.OrderUpdateStatusRequest;
import com.campusmarket.entity.Goods;
import com.campusmarket.entity.GoodsStatus;
import com.campusmarket.entity.Order;
import com.campusmarket.entity.OrderStatus;
import com.campusmarket.entity.User;
import com.campusmarket.mapper.OrderMapper;
import com.campusmarket.service.CartService;
import com.campusmarket.messaging.OrderEventPublisher;
import com.campusmarket.service.GoodsMetricsService;
import com.campusmarket.service.GoodsService;
import com.campusmarket.service.OrderService;
import com.campusmarket.service.UserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final String ORDER_RATE_KEY_PREFIX = "rate:order:";
    private static final int ORDER_RATE_LIMIT = 5;
    private static final long ORDER_RATE_WINDOW_SECONDS = 60;

    private final OrderMapper orderMapper;
    private final GoodsService goodsService;
    private final GoodsMetricsService goodsMetricsService;
    private final CartService cartService;
    private final UserService userService;
    private final StringRedisTemplate stringRedisTemplate;
    private final OrderEventPublisher orderEventPublisher;

    public OrderServiceImpl(OrderMapper orderMapper,
                            GoodsService goodsService,
                            GoodsMetricsService goodsMetricsService,
                            CartService cartService,
                            UserService userService,
                            StringRedisTemplate stringRedisTemplate,
                            OrderEventPublisher orderEventPublisher) {
        this.orderMapper = orderMapper;
        this.goodsService = goodsService;
        this.goodsMetricsService = goodsMetricsService;
        this.cartService = cartService;
        this.userService = userService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(Long buyerId, OrderCreateRequest request) {
        Assert.notNull(buyerId, "buyerId must not be null");
        enforceOrderRateLimit(buyerId);
        Goods goods = goodsService.getGoodsEntity(request.getGoodsId());
        if (Boolean.TRUE.equals(goods.getDeleted())) {
            throw new IllegalStateException("Goods has been removed");
        }
        if (!GoodsStatus.APPROVED.name().equals(goods.getStatus())) {
            throw new IllegalStateException("Goods is not available for purchase");
        }
        if (goods.getQuantity() == null || goods.getQuantity() <= 0) {
            throw new IllegalStateException("Goods is out of stock");
        }
        if (goods.getSellerId().equals(buyerId)) {
            throw new IllegalArgumentException("You cannot buy your own listing");
        }
        if (Boolean.TRUE.equals(goods.getSold())) {
            throw new IllegalStateException("Goods already sold");
        }
        userService.findById(buyerId);

        Order order = new Order();
        order.setGoodsId(goods.getId());
        order.setBuyerId(buyerId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.insert(order);
        goodsService.markSold(goods.getId(), true);
        goodsMetricsService.recordOrder(goods.getId());
        cartService.removeCartItemsForUserGoods(buyerId, goods.getId());
        orderEventPublisher.publishOrderCreated(order);
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderUpdateStatusRequest request, Long userId) {
        userService.findById(userId);
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }
        Goods goods = goodsService.getGoodsEntity(order.getGoodsId());
        boolean isBuyer = order.getBuyerId().equals(userId);
        boolean isSeller = goods.getSellerId().equals(userId);

        OrderStatus current = order.getStatus();
        OrderStatus target = request.getStatus();

        switch (target) {
            case PENDING_SHIPMENT:
                if (current != OrderStatus.PENDING_PAYMENT || !isBuyer) {
                    throw new IllegalArgumentException("Only the buyer can mark the order as paid");
                }
                order.setStatus(OrderStatus.PENDING_SHIPMENT);
                break;
            case PENDING_RECEIVE:
                if (current != OrderStatus.PENDING_SHIPMENT || !isSeller) {
                    throw new IllegalArgumentException("Only the seller can confirm shipment");
                }
                order.setStatus(OrderStatus.PENDING_RECEIVE);
                break;
            case COMPLETED:
                if (current != OrderStatus.PENDING_RECEIVE || !isBuyer) {
                    throw new IllegalArgumentException("Only the buyer can confirm receipt");
                }
                order.setStatus(OrderStatus.COMPLETED);
                break;
            case CANCELED:
                if (current != OrderStatus.PENDING_PAYMENT || !isBuyer) {
                    throw new IllegalArgumentException("Only the buyer can cancel before payment");
                }
                order.setStatus(OrderStatus.CANCELED);
                goodsService.markSold(order.getGoodsId(), false);
                cartService.removeCartItemsForUserGoods(order.getBuyerId(), order.getGoodsId());
                break;
            default:
                throw new IllegalArgumentException("Unsupported status transition");
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        orderEventPublisher.publishOrderStatusChanged(order, current);
        return toResponse(order);
    }

    @Override
    public List<OrderResponse> listOrders(Long userId) {
        userService.findById(userId);
        List<Order> buyerOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getBuyerId, userId));

        List<GoodsResponse> sellerGoods = goodsService.listGoodsBySeller(userId);
        List<Long> sellerGoodsIds = sellerGoods.stream()
                .map(GoodsResponse::getId)
                .collect(Collectors.toList());

        List<Order> sellerOrders = sellerGoodsIds.isEmpty()
                ? new ArrayList<>()
                : orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .in(Order::getGoodsId, sellerGoodsIds));

        Map<Long, Order> merged = new LinkedHashMap<>();
        buyerOrders.forEach(order -> merged.put(order.getId(), order));
        sellerOrders.forEach(order -> merged.put(order.getId(), order));

        return merged.values()
                .stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void enforceOrderRateLimit(Long userId) {
        if (stringRedisTemplate == null) {
            return;
        }
        try {
            String key = ORDER_RATE_KEY_PREFIX + userId;
            Long current = stringRedisTemplate.opsForValue().increment(key);
            if (current != null && current == 1) {
                stringRedisTemplate.expire(key, ORDER_RATE_WINDOW_SECONDS, TimeUnit.SECONDS);
            }
            if (current != null && current > ORDER_RATE_LIMIT) {
                throw new IllegalStateException("下单过于频繁，请稍后再试");
            }
        } catch (Exception ignored) {
            // 如果 Redis 异常，跳过限流逻辑，保证主流程可用
        }
    }

    private OrderResponse toResponse(Order order) {
        Goods goods = goodsService.getGoodsEntity(order.getGoodsId());
        User buyer = userService.findById(order.getBuyerId());
        User seller = userService.findById(goods.getSellerId());
        return new OrderResponse(
                order.getId(),
                order.getGoodsId(),
                order.getBuyerId(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                goods.getTitle(),
                goods.getCoverImageUrl(),
                goods.getSellerId(),
                seller.getNickname(),
                buyer.getNickname()
        );
    }
}
