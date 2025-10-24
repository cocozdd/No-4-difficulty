package com.campusmarket.messaging;

import com.campusmarket.messaging.schema.SchemaValidationUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;

class ChatEventSchemaTest {

    @Test
    void chatEventMatchesSchema() throws IOException {
        ChatEvent event = new ChatEvent(
                ChatEventType.CHAT_MESSAGE_CREATED,
                99L,
                11L,
                22L,
                "TEXT",
                "hello world",
                LocalDateTime.now()
        );

        SchemaValidationUtil.assertValid(
                "/schemas/chat-event-schema.json",
                event
        );
    }
}
