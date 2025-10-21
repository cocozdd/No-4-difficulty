package com.campusmarket.websocket.dto;

import java.time.LocalDateTime;

public class ChatConversation {

    private Long partnerId;
    private String partnerNickname;
    private String lastMessageContent;
    private LocalDateTime lastMessageAt;
    private int unreadCount;

    public ChatConversation() {
    }

    public ChatConversation(Long partnerId, String partnerNickname, String lastMessageContent, LocalDateTime lastMessageAt, int unreadCount) {
        this.partnerId = partnerId;
        this.partnerNickname = partnerNickname;
        this.lastMessageContent = lastMessageContent;
        this.lastMessageAt = lastMessageAt;
        this.unreadCount = unreadCount;
    }

    public Long getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Long partnerId) {
        this.partnerId = partnerId;
    }

    public String getPartnerNickname() {
        return partnerNickname;
    }

    public void setPartnerNickname(String partnerNickname) {
        this.partnerNickname = partnerNickname;
    }

    public String getLastMessageContent() {
        return lastMessageContent;
    }

    public void setLastMessageContent(String lastMessageContent) {
        this.lastMessageContent = lastMessageContent;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
