package com.campusmarket.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FlashSaleItemResponse {

    private Long id;
    private String title;
    private String description;
    private BigDecimal originalPrice;
    private BigDecimal flashPrice;
    private Integer totalStock;
    private Integer remainingStock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;

    public FlashSaleItemResponse(Long id,
                                 String title,
                                 String description,
                                 BigDecimal originalPrice,
                                 BigDecimal flashPrice,
                                 Integer totalStock,
                                 Integer remainingStock,
                                 LocalDateTime startTime,
                                 LocalDateTime endTime,
                                 String status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.originalPrice = originalPrice;
        this.flashPrice = flashPrice;
        this.totalStock = totalStock;
        this.remainingStock = remainingStock;
        this.startTime = startTime;
        this.endTime = endTime;
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

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public BigDecimal getFlashPrice() {
        return flashPrice;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public Integer getRemainingStock() {
        return remainingStock;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }
}
