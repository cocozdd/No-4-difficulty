package com.campusmarket.dto;

import javax.validation.constraints.Future;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FlashSaleItemCreateRequest {

    @NotBlank(message = "秒杀标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "原价不能为空")
    @Min(value = 0, message = "原价不能小于0")
    private BigDecimal originalPrice;

    @NotNull(message = "秒杀价不能为空")
    @Min(value = 0, message = "秒杀价不能小于0")
    private BigDecimal flashPrice;

    @NotNull(message = "库存不能为空")
    @Min(value = 1, message = "库存至少为1")
    private Integer totalStock;

    @NotNull(message = "开始时间不能为空")
    @Future(message = "开始时间必须晚于当前时间")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

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

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public BigDecimal getFlashPrice() {
        return flashPrice;
    }

    public void setFlashPrice(BigDecimal flashPrice) {
        this.flashPrice = flashPrice;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
