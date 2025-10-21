package com.campusmarket.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class CartItemRequest {

    @NotNull
    private Long goodsId;

    @NotNull
    @Min(1)
    private Integer quantity;

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}

