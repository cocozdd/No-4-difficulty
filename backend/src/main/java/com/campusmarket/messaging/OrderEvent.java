package com.campusmarket.messaging;

import java.time.LocalDateTime;

public class OrderEvent {

    private OrderEventType eventType;
    private Long orderId;
    private Long goodsId;
    private Long buyerId;
    private String currentStatus;
    private String previousStatus;
    private String note;
    private LocalDateTime eventTime;

    public OrderEvent() {
    }

    public OrderEvent(OrderEventType eventType,
                      Long orderId,
                      Long goodsId,
                      Long buyerId,
                      String currentStatus,
                      String previousStatus,
                      LocalDateTime eventTime) {
        this.eventType = eventType;
        this.orderId = orderId;
            this.goodsId = goodsId;
        this.buyerId = buyerId;
        this.currentStatus = currentStatus;
        this.previousStatus = previousStatus;
        this.eventTime = eventTime;
    }

    public OrderEventType getEventType() {
        return eventType;
    }

    public void setEventType(OrderEventType eventType) {
        this.eventType = eventType;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }
}
