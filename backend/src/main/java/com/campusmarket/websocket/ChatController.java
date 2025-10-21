package com.campusmarket.websocket;

import com.campusmarket.service.ChatMessageService;
import com.campusmarket.websocket.dto.ChatMessage;
import com.campusmarket.websocket.dto.ChatPayload;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import java.security.Principal;

@Controller
public class ChatController {

    private final ChatMessageService chatMessageService;

    public ChatController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @MessageMapping("/chat/send")
    public void handleChatMessage(@Payload ChatPayload payload, Principal principal) {
        if (principal == null) {
            return;
        }
        Long senderId = resolveUserId(principal);
        if (senderId == null) {
            return;
        }
        if (payload.getReceiverId() == null || !StringUtils.hasText(payload.getContent())) {
            return;
        }
        String sanitizedContent = payload.getContent().trim();
        if (sanitizedContent.isEmpty()) {
            return;
        }

        ChatMessage response = chatMessageService.saveAndBroadcast(
                senderId,
                payload.getReceiverId(),
                sanitizedContent,
                payload.getMessageType()
        );
    }

    private Long resolveUserId(Principal principal) {
        if (principal instanceof StompPrincipal) {
            return ((StompPrincipal) principal).getUserId();
        }
        String name = principal.getName();
        if (!StringUtils.hasText(name)) {
            return null;
        }
        try {
            return Long.valueOf(name);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
