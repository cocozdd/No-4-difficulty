package com.campusmarket.messaging;

import com.campusmarket.service.impl.ChatEventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ChatEventListener {

    private static final Logger log = LoggerFactory.getLogger(ChatEventListener.class);

    private final ChatEventProcessor chatEventProcessor;

    public ChatEventListener(ChatEventProcessor chatEventProcessor) {
        this.chatEventProcessor = chatEventProcessor;
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
        chatEventProcessor.handle(event);
    }
}
