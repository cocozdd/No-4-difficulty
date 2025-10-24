package com.campusmarket.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ChatEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ChatEventPublisher.class);

    private final KafkaTemplate<String, ChatEvent> kafkaTemplate;
    private final String chatTopic;

    public ChatEventPublisher(KafkaTemplate<String, ChatEvent> kafkaTemplate,
                              @Value("${app.kafka.chat-topic:chat-events}") String chatTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.chatTopic = chatTopic;
    }

    public void publishMessageCreated(Long messageId,
                                      Long senderId,
                                      Long receiverId,
                                      String messageType,
                                      String contentPreview) {
        ChatEvent event = new ChatEvent(
                ChatEventType.CHAT_MESSAGE_CREATED,
                messageId,
                senderId,
                receiverId,
                messageType,
                contentPreview,
                LocalDateTime.now()
        );
        String key = messageId == null ? null : messageId.toString();
        kafkaTemplate.send(chatTopic, key, event)
                .addCallback(result -> {
                            if (log.isDebugEnabled() && result != null) {
                                log.debug("Published chat event: messageId={}, partition={}, offset={}",
                                        event.getMessageId(),
                                        result.getRecordMetadata().partition(),
                                        result.getRecordMetadata().offset());
                            }
                        },
                        throwable -> log.warn("Failed to publish chat event: {}", event, throwable));
    }
}
