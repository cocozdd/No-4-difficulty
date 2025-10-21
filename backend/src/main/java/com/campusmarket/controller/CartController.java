package com.campusmarket.controller;

import com.campusmarket.dto.CartItemRequest;
import com.campusmarket.dto.CartItemResponse;
import com.campusmarket.dto.CartItemUpdateRequest;
import com.campusmarket.entity.User;
import com.campusmarket.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public List<CartItemResponse> listCartItems(@AuthenticationPrincipal User user) {
        return cartService.listCartItems(user.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartItemResponse addCartItem(@AuthenticationPrincipal User user,
                                        @Validated @RequestBody CartItemRequest request) {
        return cartService.addCartItem(user.getId(), request);
    }

    @PutMapping("/{cartItemId}")
    public CartItemResponse updateCartItem(@AuthenticationPrincipal User user,
                                           @PathVariable Long cartItemId,
                                           @Validated @RequestBody CartItemUpdateRequest request) {
        return cartService.updateCartItem(user.getId(), cartItemId, request);
    }

    @DeleteMapping("/{cartItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCartItem(@AuthenticationPrincipal User user,
                               @PathVariable Long cartItemId) {
        cartService.removeCartItem(user.getId(), cartItemId);
    }

    @DeleteMapping("/sold")
    public Map<String, Integer> removeSold(@AuthenticationPrincipal User user) {
        int removed = cartService.removeSoldItems(user.getId());
        return Map.of("removed", removed);
    }
}

