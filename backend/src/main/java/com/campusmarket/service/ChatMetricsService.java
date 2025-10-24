package com.campusmarket.service;

public interface ChatMetricsService {

    void recordMessageCreated(Long senderId, Long receiverId);
}
