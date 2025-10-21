package com.campusmarket.websocket.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class ChatPayload {

    @NotNull
    private Long receiverId;

    @NotBlank
    private String content;
    private String messageType = "TEXT";

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
}
