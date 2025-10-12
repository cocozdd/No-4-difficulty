package com.campusmarket.dto;

import com.campusmarket.entity.OrderStatus;

import javax.validation.constraints.NotNull;

public class OrderUpdateStatusRequest {

    @NotNull(message = "Status must not be null")
    private OrderStatus status;

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
