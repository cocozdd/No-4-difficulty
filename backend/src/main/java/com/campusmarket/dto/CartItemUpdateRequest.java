package com.campusmarket.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class CartItemUpdateRequest {

    @NotNull
    @Min(1)
    private Integer quantity;

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}

