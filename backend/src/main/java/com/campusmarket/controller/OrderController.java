package com.campusmarket.controller;

import com.campusmarket.dto.OrderCreateRequest;
import com.campusmarket.dto.OrderResponse;
import com.campusmarket.dto.OrderUpdateStatusRequest;
import com.campusmarket.entity.User;
import com.campusmarket.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public OrderResponse createOrder(@RequestBody @Validated OrderCreateRequest request,
                                     @AuthenticationPrincipal User user) {
        return orderService.createOrder(user.getId(), request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public List<OrderResponse> listOrders(@AuthenticationPrincipal User user) {
        return orderService.listOrders(user.getId());
    }

    @PatchMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public OrderResponse updateStatus(@PathVariable Long orderId,
                                      @RequestBody @Validated OrderUpdateStatusRequest request,
                                      @AuthenticationPrincipal User user) {
        return orderService.updateStatus(orderId, request, user.getId());
    }
}
