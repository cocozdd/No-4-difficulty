package com.campusmarket.dto;

import com.campusmarket.entity.OrderStatus;

import java.time.LocalDateTime;

public class OrderResponse {
    private Long id;
    private Long goodsId;
    private Long buyerId;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String goodsTitle;
    private String goodsCoverImageUrl;
    private Long sellerId;
    private String sellerNickname;
    private String buyerNickname;

    public OrderResponse(Long id,
                         Long goodsId,
                         Long buyerId,
                         OrderStatus status,
                         LocalDateTime createdAt,
                         LocalDateTime updatedAt,
                         String goodsTitle,
                         String goodsCoverImageUrl,
                         Long sellerId,
                         String sellerNickname,
                         String buyerNickname) {
        this.id = id;
        this.goodsId = goodsId;
        this.buyerId = buyerId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.goodsTitle = goodsTitle;
        this.goodsCoverImageUrl = goodsCoverImageUrl;
        this.sellerId = sellerId;
        this.sellerNickname = sellerNickname;
        this.buyerNickname = buyerNickname;
    }

    public Long getId() {
        return id;
    }

    public Long getGoodsId() {
        return goodsId;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getGoodsTitle() {
        return goodsTitle;
    }

    public String getGoodsCoverImageUrl() {
        return goodsCoverImageUrl;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public String getSellerNickname() {
        return sellerNickname;
    }

    public String getBuyerNickname() {
        return buyerNickname;
    }
}
