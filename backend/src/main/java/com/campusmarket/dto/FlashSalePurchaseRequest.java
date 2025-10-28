package com.campusmarket.dto;

import javax.validation.constraints.NotNull;

public class FlashSalePurchaseRequest {

    @NotNull(message = "秒杀活动ID不能为空")
    private Long flashSaleItemId;

    public Long getFlashSaleItemId() {
        return flashSaleItemId;
    }

    public void setFlashSaleItemId(Long flashSaleItemId) {
        this.flashSaleItemId = flashSaleItemId;
    }
}
