package com.campusmarket.dto;

import java.math.BigDecimal;

public class CartItemResponse {

    private Long id;
    private Long goodsId;
    private String goodsTitle;
    private String goodsCoverImageUrl;
    private String goodsCategory;
    private BigDecimal goodsPrice;
    private Integer goodsQuantityAvailable;
    private boolean goodsSold;
    private String goodsStatus;
    private Integer quantity;

    public CartItemResponse() {
    }

    public CartItemResponse(Long id,
                            Long goodsId,
                            String goodsTitle,
                            String goodsCoverImageUrl,
                            String goodsCategory,
                            BigDecimal goodsPrice,
                            Integer goodsQuantityAvailable,
                            boolean goodsSold,
                            String goodsStatus,
                            Integer quantity) {
        this.id = id;
        this.goodsId = goodsId;
        this.goodsTitle = goodsTitle;
        this.goodsCoverImageUrl = goodsCoverImageUrl;
        this.goodsCategory = goodsCategory;
        this.goodsPrice = goodsPrice;
        this.goodsQuantityAvailable = goodsQuantityAvailable;
        this.goodsSold = goodsSold;
        this.goodsStatus = goodsStatus;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }

    public String getGoodsTitle() {
        return goodsTitle;
    }

    public void setGoodsTitle(String goodsTitle) {
        this.goodsTitle = goodsTitle;
    }

    public String getGoodsCoverImageUrl() {
        return goodsCoverImageUrl;
    }

    public void setGoodsCoverImageUrl(String goodsCoverImageUrl) {
        this.goodsCoverImageUrl = goodsCoverImageUrl;
    }

    public String getGoodsCategory() {
        return goodsCategory;
    }

    public void setGoodsCategory(String goodsCategory) {
        this.goodsCategory = goodsCategory;
    }

    public BigDecimal getGoodsPrice() {
        return goodsPrice;
    }

    public void setGoodsPrice(BigDecimal goodsPrice) {
        this.goodsPrice = goodsPrice;
    }

    public Integer getGoodsQuantityAvailable() {
        return goodsQuantityAvailable;
    }

    public void setGoodsQuantityAvailable(Integer goodsQuantityAvailable) {
        this.goodsQuantityAvailable = goodsQuantityAvailable;
    }

    public boolean isGoodsSold() {
        return goodsSold;
    }

    public void setGoodsSold(boolean goodsSold) {
        this.goodsSold = goodsSold;
    }

    public String getGoodsStatus() {
        return goodsStatus;
    }

    public void setGoodsStatus(String goodsStatus) {
        this.goodsStatus = goodsStatus;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
