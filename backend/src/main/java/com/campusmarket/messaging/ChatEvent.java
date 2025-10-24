package com.campusmarket.messaging;

import java.time.LocalDateTime;

public class ChatEvent {

    private ChatEventType eventType;
    private Long messageId;
    private Long senderId;
    private Long receiverId;
    private String messageType;
    private String contentPreview;
    private LocalDateTime eventTime;

    public ChatEvent() {
    }

    public ChatEvent(ChatEventType eventType,
                     Long messageId,
                     Long senderId,
                     Long receiverId,
                     String messageType,
                     String contentPreview,
                     LocalDateTime eventTime) {
        this.eventType = eventType;
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageType = messageType;
        this.contentPreview = contentPreview;
        this.eventTime = eventTime;
    }

    public ChatEventType getEventType() {
        return eventType;
    }

    public void setEventType(ChatEventType eventType) {
        this.eventType = eventType;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getContentPreview() {
        return contentPreview;
    }

    public void setContentPreview(String contentPreview) {
        this.contentPreview = contentPreview;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }
}
