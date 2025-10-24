package com.campusmarket.dto;

import javax.validation.constraints.Size;

public class KafkaTestRequest {

    @Size(max = 120, message = "Key cannot exceed 120 characters")
    private String key;

    private Long orderId;

    private Long goodsId;

    private Long buyerId;

    @Size(max = 120, message = "Current status cannot exceed 120 characters")
    private String currentStatus;

    @Size(max = 120, message = "Previous status cannot exceed 120 characters")
    private String previousStatus;

    @Size(max = 255, message = "Message cannot exceed 255 characters")
    private String message;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
