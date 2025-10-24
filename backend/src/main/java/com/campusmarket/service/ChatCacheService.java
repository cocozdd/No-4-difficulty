package com.campusmarket.service;

import java.util.Map;

/**
 * Manage chat-related cache entries such as unread counters.
 */
public interface ChatCacheService {

    /**
     * Increment unread counters for the receiver/sender pair if the message has not been processed.
     *
     * @param receiverId the message receiver
     * @param senderId   the message sender
     * @param messageId  unique message identifier used for idempotency
     */
    void incrementUnread(Long receiverId, Long senderId, Long messageId);

    /**
     * Clear unread counters for a conversation.
     *
     * @param userId    the user clearing the unread count
     * @param partnerId the conversation partner
     */
    void clearUnread(Long userId, Long partnerId);

    /**
     * Return unread counts for all partners of the user.
     *
     * @param userId the user
     * @return partnerId -> unread count map (empty when missing)
     */
    Map<Long, Integer> getUnreadCounts(Long userId);

    /**
     * Convenience accessor for a single partner.
     *
     * @param userId    the user
     * @param partnerId partner identifier
     * @return unread count (0 when missing)
     */
    default int getUnreadCount(Long userId, Long partnerId) {
        return getUnreadCounts(userId).getOrDefault(partnerId, 0);
    }
}
