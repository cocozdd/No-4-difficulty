package com.campusmarket.dto;

import javax.validation.constraints.NotNull;

public class OrderCreateRequest {

    @NotNull(message = "商品ID不能为空")
    private Long goodsId;

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }
}
