package com.campusmarket.service;

import com.campusmarket.dto.CartItemRequest;
import com.campusmarket.dto.CartItemResponse;
import com.campusmarket.dto.CartItemUpdateRequest;

import java.util.List;

public interface CartService {

    List<CartItemResponse> listCartItems(Long userId);

    CartItemResponse addCartItem(Long userId, CartItemRequest request);

    CartItemResponse updateCartItem(Long userId, Long cartItemId, CartItemUpdateRequest request);

    void removeCartItem(Long userId, Long cartItemId);

    void removeCartItemsByGoodsId(Long goodsId);

    void removeCartItemsForUserGoods(Long userId, Long goodsId);

    int removeSoldItems(Long userId);
}

