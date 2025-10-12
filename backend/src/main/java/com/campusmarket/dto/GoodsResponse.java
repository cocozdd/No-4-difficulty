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

    public GoodsResponse(Long id, String title, String description, String category, BigDecimal price, String coverImageUrl, LocalDateTime publishedAt, Long sellerId, String sellerNickname, boolean sold, String status) {
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
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public String getSellerNickname() {
        return sellerNickname;
    }

    public boolean isSold() {
        return sold;
    }

    public String getStatus() {
        return status;
    }
}
