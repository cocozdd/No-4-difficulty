package com.campusmarket.messaging;

import java.time.LocalDateTime;

public class GoodsEvent {

    private GoodsEventType eventType;
    private Long goodsId;
    private Long sellerId;
    private Long actorId;
    private String currentStatus;
    private Integer quantity;
    private String note;
    private LocalDateTime eventTime;

    public GoodsEvent() {
    }

    public GoodsEvent(GoodsEventType eventType,
                      Long goodsId,
                      Long sellerId,
                      Long actorId,
                      String currentStatus,
                      Integer quantity,
                      String note,
                      LocalDateTime eventTime) {
        this.eventType = eventType;
        this.goodsId = goodsId;
        this.sellerId = sellerId;
        this.actorId = actorId;
        this.currentStatus = currentStatus;
        this.quantity = quantity;
        this.note = note;
        this.eventTime = eventTime;
    }

    public GoodsEventType getEventType() {
        return eventType;
    }

    public void setEventType(GoodsEventType eventType) {
        this.eventType = eventType;
    }

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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
