package com.campusmarket.dto;

import java.time.LocalDateTime;

public class KafkaTestResponse {

    private String topic;
    private String key;
    private LocalDateTime dispatchedAt;
    private String message;

    public KafkaTestResponse() {
    }

    public KafkaTestResponse(String topic, String key, LocalDateTime dispatchedAt, String message) {
        this.topic = topic;
        this.key = key;
        this.dispatchedAt = dispatchedAt;
        this.message = message;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(LocalDateTime dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
