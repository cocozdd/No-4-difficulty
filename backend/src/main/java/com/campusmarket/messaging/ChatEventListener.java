package com.campusmarket.messaging;

import com.campusmarket.service.ChatMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ChatEventListener {

    private static final Logger log = LoggerFactory.getLogger(ChatEventListener.class);

    private final ChatMetricsService chatMetricsService;

    public ChatEventListener(ChatMetricsService chatMetricsService) {
        this.chatMetricsService = chatMetricsService;
    }

    @KafkaListener(
            topics = "${app.kafka.chat-topic:chat-events}",
            groupId = "${spring.kafka.consumer.group-id}-chat"
    )
    public void handleChatEvent(ChatEvent event) {
        log.info("Received chat event: type={}, messageId={}, sender={}, receiver={}, preview={}",
                event.getEventType(),
                event.getMessageId(),
                event.getSenderId(),
                event.getReceiverId(),
                event.getContentPreview());
        chatMetricsService.recordMessageCreated(event.getSenderId(), event.getReceiverId());
    }
}
