package com.campusmarket.dto;

import com.campusmarket.entity.GoodsStatus;

import javax.validation.constraints.NotNull;

public class GoodsReviewRequest {

    @NotNull(message = "Review status must not be null")
    private GoodsStatus status;

    public GoodsStatus getStatus() {
        return status;
    }

    public void setStatus(GoodsStatus status) {
        this.status = status;
    }
}
