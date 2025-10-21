package com.campusmarket.controller;

import com.campusmarket.service.ChatMessageService;
import com.campusmarket.websocket.dto.ChatConversation;
import com.campusmarket.websocket.dto.ChatMessage;
import com.campusmarket.websocket.dto.ChatPayload;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
public class ChatControllerRest {

    private final ChatMessageService chatMessageService;

    public ChatControllerRest(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @GetMapping("/conversations")
    public List<ChatConversation> listConversations(@AuthenticationPrincipal com.campusmarket.entity.User user) {
        return chatMessageService.listConversations(user.getId());
    }

    @GetMapping("/messages")
    public List<ChatMessage> getMessages(@AuthenticationPrincipal com.campusmarket.entity.User user,
                                         @RequestParam Long partnerId) {
        return chatMessageService.getConversation(user.getId(), partnerId);
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessage sendMessage(@AuthenticationPrincipal com.campusmarket.entity.User user,
                                   @Valid @RequestBody ChatPayload payload) {
        String content = payload.getContent() != null ? payload.getContent().trim() : "";
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        return chatMessageService.saveAndBroadcast(
                user.getId(),
                payload.getReceiverId(),
                content,
                payload.getMessageType()
        );
    }

    @PostMapping("/read/{partnerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal com.campusmarket.entity.User user,
                         @PathVariable Long partnerId) {
        chatMessageService.markConversationRead(user.getId(), partnerId);
    }
}
