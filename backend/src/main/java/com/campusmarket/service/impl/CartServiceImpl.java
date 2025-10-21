package com.campusmarket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusmarket.dto.CartItemRequest;
import com.campusmarket.dto.CartItemResponse;
import com.campusmarket.dto.CartItemUpdateRequest;
import com.campusmarket.entity.CartItem;
import com.campusmarket.entity.Goods;
import com.campusmarket.entity.GoodsStatus;
import com.campusmarket.mapper.CartItemMapper;
import com.campusmarket.mapper.GoodsMapper;
import com.campusmarket.service.CartService;
import com.campusmarket.service.GoodsMetricsService;
import com.campusmarket.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private final CartItemMapper cartItemMapper;
    private final GoodsMapper goodsMapper;
    private final UserService userService;
    private final GoodsMetricsService goodsMetricsService;

    public CartServiceImpl(CartItemMapper cartItemMapper,
                           GoodsMapper goodsMapper,
                           UserService userService,
                           GoodsMetricsService goodsMetricsService) {
        this.cartItemMapper = cartItemMapper;
        this.goodsMapper = goodsMapper;
        this.userService = userService;
        this.goodsMetricsService = goodsMetricsService;
    }

    @Override
    public List<CartItemResponse> listCartItems(Long userId) {
        userService.findById(userId);
        List<CartItem> cartItems = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getUpdatedAt));
        if (CollectionUtils.isEmpty(cartItems)) {
            return Collections.emptyList();
        }
        Map<Long, Goods> goodsMap = fetchGoodsMap(cartItems);
        return cartItems.stream()
                .map(item -> toResponse(item, goodsMap.get(item.getGoodsId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CartItemResponse addCartItem(Long userId, CartItemRequest request) {
        Assert.notNull(userId, "userId must not be null");
        userService.findById(userId);
        Goods goods = validateGoodsForCart(request.getGoodsId(), request.getQuantity(), userId);

        CartItem existing = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getGoodsId, request.getGoodsId()));
        if (existing != null) {
            int newQuantity = existing.getQuantity() + request.getQuantity();
            if (newQuantity > goods.getQuantity()) {
                throw new IllegalArgumentException("Quantity exceeds available stock");
            }
            existing.setQuantity(newQuantity);
            existing.setUpdatedAt(LocalDateTime.now());
            cartItemMapper.updateById(existing);
            goodsMetricsService.recordCartAddition(request.getGoodsId());
            return toResponse(existing, goods);
        }

        CartItem cartItem = new CartItem();
        cartItem.setUserId(userId);
        cartItem.setGoodsId(request.getGoodsId());
        cartItem.setQuantity(request.getQuantity());
        cartItem.setCreatedAt(LocalDateTime.now());
        cartItem.setUpdatedAt(LocalDateTime.now());
        cartItemMapper.insert(cartItem);
        goodsMetricsService.recordCartAddition(request.getGoodsId());
        return toResponse(cartItem, goods);
    }

    @Override
    @Transactional
    public CartItemResponse updateCartItem(Long userId, Long cartItemId, CartItemUpdateRequest request) {
        userService.findById(userId);
        CartItem cartItem = cartItemMapper.selectById(cartItemId);
        if (cartItem == null || !cartItem.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Cart item not found");
        }
        Goods goods = validateGoodsForCart(cartItem.getGoodsId(), request.getQuantity(), userId);

        cartItem.setQuantity(request.getQuantity());
        cartItem.setUpdatedAt(LocalDateTime.now());
        cartItemMapper.updateById(cartItem);
        return toResponse(cartItem, goods);
    }

    @Override
    @Transactional
    public void removeCartItem(Long userId, Long cartItemId) {
        userService.findById(userId);
        CartItem cartItem = cartItemMapper.selectById(cartItemId);
        if (cartItem == null || !cartItem.getUserId().equals(userId)) {
            return;
        }
        cartItemMapper.deleteById(cartItemId);
    }

    @Override
    @Transactional
    public void removeCartItemsByGoodsId(Long goodsId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getGoodsId, goodsId));
    }

    @Override
    @Transactional
    public void removeCartItemsForUserGoods(Long userId, Long goodsId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getGoodsId, goodsId));
    }

    @Override
    @Transactional
    public int removeSoldItems(Long userId) {
        List<CartItem> cartItems = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId));
        if (CollectionUtils.isEmpty(cartItems)) {
            return 0;
        }
        Map<Long, Goods> goodsMap = fetchGoodsMap(cartItems);
        List<Long> soldCartIds = cartItems.stream()
                .filter(item -> {
                    Goods goods = goodsMap.get(item.getGoodsId());
                    if (goods == null) {
                        return true;
                    }
                    boolean sold = Boolean.TRUE.equals(goods.getSold()) || goods.getQuantity() == null || goods.getQuantity() <= 0;
                    boolean inactiveStatus = !GoodsStatus.APPROVED.name().equals(goods.getStatus());
                    return sold || inactiveStatus;
                })
                .map(CartItem::getId)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(soldCartIds)) {
            return 0;
        }
        return cartItemMapper.deleteBatchIds(soldCartIds);
    }

    private Map<Long, Goods> fetchGoodsMap(List<CartItem> cartItems) {
        List<Long> goodsIds = cartItems.stream()
                .map(CartItem::getGoodsId)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(goodsIds)) {
            return Collections.emptyMap();
        }
        return goodsMapper.selectBatchIds(goodsIds)
                .stream()
                .collect(Collectors.toMap(Goods::getId, goods -> goods, (left, right) -> left, HashMap::new));
    }

    private CartItemResponse toResponse(CartItem cartItem, Goods goods) {
        if (goods == null) {
            return new CartItemResponse(
                    cartItem.getId(),
                    cartItem.getGoodsId(),
                    "Unavailable",
                    null,
                    null,
                    null,
                    0,
                    true,
                    "REMOVED",
                    cartItem.getQuantity()
            );
        }
        return new CartItemResponse(
                cartItem.getId(),
                goods.getId(),
                goods.getTitle(),
                goods.getCoverImageUrl(),
                goods.getCategory(),
                goods.getPrice(),
                goods.getQuantity(),
                Boolean.TRUE.equals(goods.getSold()),
                goods.getStatus(),
                cartItem.getQuantity()
        );
    }

    private Goods validateGoodsForCart(Long goodsId, Integer quantity, Long userId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null || Boolean.TRUE.equals(goods.getDeleted())) {
            throw new IllegalArgumentException("Goods not found");
        }
        if (goods.getSellerId().equals(userId)) {
            throw new IllegalArgumentException("You cannot add your own goods to cart");
        }
        if (!GoodsStatus.APPROVED.name().equals(goods.getStatus())) {
            throw new IllegalArgumentException("Goods is not available for purchase");
        }
        if (Boolean.TRUE.equals(goods.getSold()) || goods.getQuantity() == null || goods.getQuantity() <= 0) {
            throw new IllegalArgumentException("Goods is out of stock");
        }
        if (quantity > goods.getQuantity()) {
            throw new IllegalArgumentException("Quantity exceeds available stock");
        }
        return goods;
    }
}
