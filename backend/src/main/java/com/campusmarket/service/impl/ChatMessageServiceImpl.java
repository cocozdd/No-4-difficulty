package com.campusmarket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campusmarket.entity.ChatMessageEntity;
import com.campusmarket.entity.User;
import com.campusmarket.mapper.ChatMessageMapper;
import com.campusmarket.messaging.ChatEventPublisher;
import com.campusmarket.service.ChatCacheService;
import com.campusmarket.service.ChatMessageService;
import com.campusmarket.service.UserService;
import com.campusmarket.websocket.dto.ChatConversation;
import com.campusmarket.websocket.dto.ChatMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageMapper chatMessageMapper;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatCacheService chatCacheService;
    private final ChatEventPublisher chatEventPublisher;

    public ChatMessageServiceImpl(ChatMessageMapper chatMessageMapper,
                                  UserService userService,
                                  SimpMessagingTemplate messagingTemplate,
                                  ChatCacheService chatCacheService,
                                  ChatEventPublisher chatEventPublisher) {
        this.chatMessageMapper = chatMessageMapper;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
        this.chatCacheService = chatCacheService;
        this.chatEventPublisher = chatEventPublisher;
    }

    @Override
    @Transactional
    public ChatMessage saveAndBroadcast(Long senderId, Long receiverId, String content, String messageType) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setSenderId(senderId);
        entity.setReceiverId(receiverId);
        entity.setContent(content);
        entity.setMessageType(normalizeMessageType(messageType));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setReadAt(null);
        chatMessageMapper.insert(entity);
        chatEventPublisher.publishMessageCreated(
                entity.getId(),
                senderId,
                receiverId,
                entity.getMessageType(),
                buildPreview(entity)
        );

        Map<Long, User> cache = new HashMap<>();
        ChatMessage senderMessage = toDto(entity, senderId, true, cache);
        ChatMessage receiverMessage = toDto(entity, receiverId, false, cache);

        messagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/messages", receiverMessage);
        messagingTemplate.convertAndSendToUser(senderId.toString(), "/queue/messages", senderMessage);
        return senderMessage;
    }

    @Override
    public List<ChatMessage> getConversation(Long userId, Long partnerId) {
        List<ChatMessageEntity> entities = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessageEntity>()
                .and(wrapper -> wrapper.eq(ChatMessageEntity::getSenderId, userId)
                        .eq(ChatMessageEntity::getReceiverId, partnerId))
                .or(wrapper -> wrapper.eq(ChatMessageEntity::getSenderId, partnerId)
                        .eq(ChatMessageEntity::getReceiverId, userId))
                .orderByAsc(ChatMessageEntity::getCreatedAt));
        Map<Long, User> cache = new HashMap<>();
        return entities.stream()
                .map(entity -> toDto(entity, userId, determineReadForUser(entity, userId), cache))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatConversation> listConversations(Long userId) {
        List<ChatMessageEntity> entities = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getSenderId, userId)
                .or(wrapper -> wrapper.eq(ChatMessageEntity::getReceiverId, userId)));
        if (CollectionUtils.isEmpty(entities)) {
            return new ArrayList<>();
        }
        entities.sort(Comparator.comparing(ChatMessageEntity::getCreatedAt).reversed());

        Map<Long, ChatConversation> conversations = new HashMap<>();
        Map<Long, Integer> unreadCountMap = new HashMap<>();
        Map<Long, Integer> cachedUnreadCounts = chatCacheService.getUnreadCounts(userId);

        for (ChatMessageEntity entity : entities) {
            Long partnerId = entity.getSenderId().equals(userId) ? entity.getReceiverId() : entity.getSenderId();
            ChatConversation existing = conversations.get(partnerId);
            if (existing == null) {
                conversations.put(partnerId, new ChatConversation(
                        partnerId,
                        null,
                        buildPreview(entity),
                        entity.getCreatedAt(),
                        0
                ));
            } else if (existing.getLastMessageAt().isBefore(entity.getCreatedAt())) {
                existing.setLastMessageAt(entity.getCreatedAt());
                existing.setLastMessageContent(buildPreview(entity));
            }
            if (entity.getReceiverId().equals(userId) && entity.getReadAt() == null) {
                unreadCountMap.merge(partnerId, 1, Integer::sum);
            }
        }

        Set<Long> partnerIds = conversations.keySet();
        Map<Long, String> nicknameMap = fetchNicknames(partnerIds);

        return conversations.values().stream()
                .peek(conv -> {
                    conv.setPartnerNickname(nicknameMap.getOrDefault(conv.getPartnerId(), ""));
                    int dbUnread = unreadCountMap.getOrDefault(conv.getPartnerId(), 0);
                    int redisUnread = cachedUnreadCounts.getOrDefault(conv.getPartnerId(), 0);
                    conv.setUnreadCount(Math.max(dbUnread, redisUnread));
                })
                .sorted(Comparator.comparing(ChatConversation::getLastMessageAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markConversationRead(Long userId, Long partnerId) {
        chatMessageMapper.update(null, new LambdaUpdateWrapper<ChatMessageEntity>()
                .set(ChatMessageEntity::getReadAt, LocalDateTime.now())
                .eq(ChatMessageEntity::getReceiverId, userId)
                .eq(ChatMessageEntity::getSenderId, partnerId)
                .isNull(ChatMessageEntity::getReadAt));
        chatCacheService.clearUnread(userId, partnerId);
    }

    private String buildPreview(ChatMessageEntity entity) {
        String type = normalizeMessageType(entity.getMessageType());
        if ("IMAGE".equals(type)) {
            return "[Image]";
        }
        String content = entity.getContent();
        if (content == null) {
            return "";
        }
        return content.length() > 60 ? content.substring(0, 60) + "..." : content;
    }


    private Map<Long, String> fetchNicknames(Set<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return new HashMap<>();
        }
        Map<Long, String> result = new HashMap<>();
        for (Long id : userIds) {
            try {
                User user = userService.findById(id);
                result.put(id, user.getNickname());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    private ChatMessage toDto(ChatMessageEntity entity, Long viewerId, boolean readForViewer) {
        return toDto(entity, viewerId, readForViewer, new HashMap<>());
    }

    private ChatMessage toDto(ChatMessageEntity entity, Long viewerId, boolean readForViewer, Map<Long, User> cache) {
        User sender = getCachedUser(entity.getSenderId(), cache);
        User receiver = getCachedUser(entity.getReceiverId(), cache);
        ChatMessage dto = new ChatMessage();
        dto.setId(entity.getId());
        dto.setSenderId(entity.getSenderId());
        dto.setReceiverId(entity.getReceiverId());
        dto.setSenderNickname(sender.getNickname());
        dto.setReceiverNickname(receiver.getNickname());
        dto.setContent(entity.getContent());
        dto.setType(normalizeMessageType(entity.getMessageType()));
        dto.setTimestamp(entity.getCreatedAt());
        dto.setRead(readForViewer);
        dto.setReadAt(entity.getReadAt());
        return dto;
    }

    private User getCachedUser(Long userId, Map<Long, User> cache) {
        return cache.computeIfAbsent(userId, userService::findById);
    }

    private boolean determineReadForUser(ChatMessageEntity entity, Long userId) {
        if (entity.getReceiverId().equals(userId)) {
            return entity.getReadAt() != null;
        }
        return true;
    }

    private String normalizeMessageType(String messageType) {
        if (messageType == null) {
            return "TEXT";
        }
        String upper = messageType.trim().toUpperCase();
        if ("IMAGE".equals(upper)) {
            return "IMAGE";
        }
        return "TEXT";
    }

}
