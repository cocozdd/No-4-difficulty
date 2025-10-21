import request from './request';

export interface ChatMessage {
  id: number;
  senderId: number;
  receiverId: number;
  senderNickname: string;
  receiverNickname: string;
  content: string;
  messageType: 'TEXT' | 'IMAGE';
  timestamp: string;
  read: boolean;
  readAt?: string | null;
}

export interface ChatConversation {
  partnerId: number;
  partnerNickname: string;
  lastMessageContent: string;
  messageType: 'TEXT' | 'IMAGE';
  lastMessageAt: string;
  unreadCount: number;
}

export interface SendChatMessagePayload {
  receiverId: number;
  content: string;
  messageType: 'TEXT' | 'IMAGE';
}

export const fetchConversations = () =>
  request.get<ChatConversation[]>('/chat/conversations');

export const fetchMessages = (partnerId: number) =>
  request.get<ChatMessage[]>('/chat/messages', { params: { partnerId } });

export const markConversationRead = (partnerId: number) =>
  request.post(`/chat/read/${partnerId}`);

export const sendChatMessage = (payload: SendChatMessagePayload) =>
  request.post<ChatMessage>('/chat/messages', payload);


