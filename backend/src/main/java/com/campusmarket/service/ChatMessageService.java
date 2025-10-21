package com.campusmarket.service;

import com.campusmarket.websocket.dto.ChatMessage;
import com.campusmarket.websocket.dto.ChatConversation;

import java.util.List;

public interface ChatMessageService {

    ChatMessage saveAndBroadcast(Long senderId, Long receiverId, String content, String messageType);

    List<ChatMessage> getConversation(Long userId, Long partnerId);

    List<ChatConversation> listConversations(Long userId);

    void markConversationRead(Long userId, Long partnerId);
}
