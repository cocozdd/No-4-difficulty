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
import com.campusmarket.service.GoodsService;
import com.campusmarket.service.OrderService;
import com.campusmarket.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final GoodsService goodsService;
    private final UserService userService;

    public OrderServiceImpl(OrderMapper orderMapper, GoodsService goodsService, UserService userService) {
        this.orderMapper = orderMapper;
        this.goodsService = goodsService;
        this.userService = userService;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(Long buyerId, OrderCreateRequest request) {
        Assert.notNull(buyerId, "buyerId must not be null");
        Goods goods = goodsService.getGoodsEntity(request.getGoodsId());
        if (Boolean.TRUE.equals(goods.getDeleted())) {
            throw new IllegalStateException("Goods has been removed");
        }
        if (!GoodsStatus.APPROVED.name().equals(goods.getStatus())) {
            throw new IllegalStateException("Goods is not available for purchase");
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
                break;
            default:
                throw new IllegalArgumentException("Unsupported status transition");
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
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
