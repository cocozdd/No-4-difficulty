package com.campusmarket.service.impl;

import com.campusmarket.messaging.ChatEvent;
import com.campusmarket.messaging.ChatEventType;
import com.campusmarket.service.ChatCacheService;
import com.campusmarket.service.ChatMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChatEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(ChatEventProcessor.class);

    private final ChatMetricsService chatMetricsService;
    private final ChatCacheService chatCacheService;

    public ChatEventProcessor(ChatMetricsService chatMetricsService,
                              ChatCacheService chatCacheService) {
        this.chatMetricsService = chatMetricsService;
        this.chatCacheService = chatCacheService;
    }

    public void handle(ChatEvent event) {
        if (event == null || event.getEventType() == null) {
            return;
        }
        try {
            if (event.getEventType() == ChatEventType.CHAT_MESSAGE_CREATED) {
                chatMetricsService.recordMessageCreated(event.getSenderId(), event.getReceiverId());
                chatCacheService.incrementUnread(event.getReceiverId(), event.getSenderId(), event.getMessageId());
            } else {
                log.debug("Unhandled chat event type: {}", event.getEventType());
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to process chat event: {}", event, ex);
        }
    }
}
