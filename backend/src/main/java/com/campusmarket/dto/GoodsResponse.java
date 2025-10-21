package com.campusmarket.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GoodsResponse {
    private Long id;
    private String title;
    private String description;
    private String category;
    private BigDecimal price;
    private String coverImageUrl;
    private LocalDateTime publishedAt;
    private Long sellerId;
    private String sellerNickname;
    private boolean sold;
    private String status;
    private Integer quantity;

    public GoodsResponse() {
    }

    public GoodsResponse(Long id, String title, String description, String category, BigDecimal price, String coverImageUrl, LocalDateTime publishedAt, Long sellerId, String sellerNickname, boolean sold, String status, Integer quantity) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.price = price;
        this.coverImageUrl = coverImageUrl;
        this.publishedAt = publishedAt;
        this.sellerId = sellerId;
        this.sellerNickname = sellerNickname;
        this.sold = sold;
        this.status = status;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerNickname() {
        return sellerNickname;
    }

    public void setSellerNickname(String sellerNickname) {
        this.sellerNickname = sellerNickname;
    }

    public boolean isSold() {
        return sold;
    }

    public void setSold(boolean sold) {
        this.sold = sold;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
