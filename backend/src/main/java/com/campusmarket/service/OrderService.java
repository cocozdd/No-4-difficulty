package com.campusmarket.service;

import com.campusmarket.dto.OrderCreateRequest;
import com.campusmarket.dto.OrderResponse;
import com.campusmarket.dto.OrderUpdateStatusRequest;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(Long buyerId, OrderCreateRequest request);
    OrderResponse updateStatus(Long orderId, OrderUpdateStatusRequest request, Long userId);
    List<OrderResponse> listOrders(Long buyerId);
}
