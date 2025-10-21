import { Client, IMessage } from '@stomp/stompjs';
import { defineStore } from 'pinia';
import { computed, ref, watch } from 'vue';
import { useUserStore } from './userStore';
import {
  fetchConversations,
  fetchMessages,
  markConversationRead as markConversationReadApi,
  sendChatMessage,
  type ChatConversation,
  type ChatMessage
} from '../apis/chat';
import { uploadChatImage } from '../apis/upload';

interface ConversationMessages {
  [partnerId: number]: ChatMessage[];
}

interface ConversationMetaMap {
  [partnerId: number]: ChatConversation;
}

const WS_ENDPOINT_ENV = import.meta.env.VITE_WS_ENDPOINT as string | undefined;

const resolveEndpoint = () => {
  if (WS_ENDPOINT_ENV) {
    return WS_ENDPOINT_ENV;
  }
  const { protocol, hostname, port } = window.location;
  const isHttps = protocol === 'https:';
  const wsProtocol = isHttps ? 'wss:' : 'ws:';
  let targetPort = port;
  if (import.meta.env.DEV && port === '5173') {
    targetPort = '8080';
  }
  const portPart = targetPort ? `:${targetPort}` : '';
  return `${wsProtocol}//${hostname}${portPart}/ws`;
};

const sortMessages = (messages: ChatMessage[]) =>
  [...messages].sort(
    (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
  );

export const useChatStore = defineStore('chatStore', () => {
  const userStore = useUserStore();
  const client = ref<Client | null>(null);
  const connected = ref(false);
  const connecting = ref(false);
  const messageMap = ref<ConversationMessages>({});
  const conversationMeta = ref<ConversationMetaMap>({});
  const loadingConversations = ref(false);
  const loadingMessages = ref(false);

  const currentUserId = computed(() => userStore.userId ?? 0);

  const hasToken = computed(() => Boolean(userStore.token));

  const conversations = computed<ChatConversation[]>(() =>
    Object.values(conversationMeta.value).sort(
      (a, b) => new Date(b.lastMessageAt).getTime() - new Date(a.lastMessageAt).getTime()
    )
  );

const previewFromMessage = (message: ChatMessage) =>
  message.type === 'IMAGE' ? '[Image]' : message.content;

const conversationFor = (partnerId: number) => messageMap.value[partnerId] ?? [];

  const unreadCount = (partnerId: number) =>
    conversationMeta.value[partnerId]?.unreadCount ?? 0;

  const ensureMeta = (partnerId: number, defaults?: Partial<ChatConversation>) => {
    if (!conversationMeta.value[partnerId]) {
      conversationMeta.value = {
        ...conversationMeta.value,
        [partnerId]: {
          partnerId,
          partnerNickname: defaults?.partnerNickname ?? '',
          lastMessageContent: defaults?.lastMessageContent ?? '',
          lastMessageAt: defaults?.lastMessageAt ?? new Date().toISOString(),
          unreadCount: defaults?.unreadCount ?? 0
        }
      };
    } else if (defaults) {
      conversationMeta.value = {
        ...conversationMeta.value,
        [partnerId]: {
          ...conversationMeta.value[partnerId],
          ...defaults
        }
      };
    }
  };

  const updateMetaWithMessage = (message: ChatMessage) => {
    const selfId = currentUserId.value;
    const partnerId = message.senderId === selfId ? message.receiverId : message.senderId;
    const isIncoming = message.receiverId === selfId;
    const meta = conversationMeta.value[partnerId];
    const nickname = isIncoming ? message.senderNickname : message.receiverNickname;

    const unreadIncrement = isIncoming && !message.read ? 1 : 0;

    ensureMeta(partnerId, {
      partnerNickname: nickname || meta?.partnerNickname || '',
      lastMessageContent: previewFromMessage(message),
      lastMessageAt: message.timestamp,
      unreadCount: (meta?.unreadCount ?? 0) + unreadIncrement
    });
  };

  const insertMessage = (message: ChatMessage) => {
    const selfId = currentUserId.value;
    const partnerId = message.senderId === selfId ? message.receiverId : message.senderId;
    const existing = messageMap.value[partnerId] ? [...messageMap.value[partnerId]] : [];
    const existingIndex = existing.findIndex((item) => item.id === message.id);
    if (existingIndex >= 0) {
      existing[existingIndex] = message;
    } else {
      existing.push(message);
    }
    messageMap.value = {
      ...messageMap.value,
      [partnerId]: sortMessages(existing)
    };
    updateMetaWithMessage(message);
  };

  const handleIncomingMessage = (rawMessage: IMessage) => {
    try {
      const data: ChatMessage = JSON.parse(rawMessage.body);
      insertMessage(data);
    } catch (error) {
      if (import.meta.env.DEV) {
        console.error('[chat] Failed to parse incoming message', error);
      }
    }
  };

  const connect = () => {
    if (connected.value || connecting.value) {
      return;
    }
    if (!userStore.token) {
      return;
    }
    connecting.value = true;
    const endpoint = resolveEndpoint();
    const stompClient = new Client({
      brokerURL: `${endpoint}?token=${encodeURIComponent(userStore.token)}`,
      reconnectDelay: 5000,
      debug: (message) => {
        if (import.meta.env.DEV) {
          console.debug('[stomp]', message);
        }
      }
    });

    stompClient.onConnect = () => {
      connected.value = true;
      connecting.value = false;
      stompClient.subscribe('/user/queue/messages', handleIncomingMessage);
    };

    stompClient.onStompError = (frame) => {
      if (import.meta.env.DEV) {
        console.error('[stomp] Broker error', frame.headers['message'], frame.body);
      }
    };

    stompClient.onWebSocketClose = () => {
      connected.value = false;
      connecting.value = false;
    };

    stompClient.onWebSocketError = () => {
      connected.value = false;
      connecting.value = false;
    };

    stompClient.activate();
    client.value = stompClient;
  };

  const disconnect = (clearHistory = false) => {
    connected.value = false;
    connecting.value = false;
    if (client.value) {
      client.value.deactivate();
      client.value = null;
    }
    if (clearHistory) {
      messageMap.value = {};
      conversationMeta.value = {};
    }
  };

  const refreshConversations = async () => {
    if (!hasToken.value) {
      return;
    }
    loadingConversations.value = true;
    try {
      const { data } = await fetchConversations();
      const meta: ConversationMetaMap = {};
      data.forEach((item) => {
        meta[item.partnerId] = item;
      });
      conversationMeta.value = meta;
    } finally {
      loadingConversations.value = false;
    }
  };

  const loadConversationMessages = async (partnerId: number) => {
    if (!hasToken.value) {
      return;
    }
    loadingMessages.value = true;
    try {
      const { data } = await fetchMessages(partnerId);
      messageMap.value = {
        ...messageMap.value,
        [partnerId]: sortMessages(data)
      };
      if (!conversationMeta.value[partnerId]) {
        const last = data[data.length - 1];
        if (last) {
          ensureMeta(partnerId, {
            partnerNickname:
              last.senderId === currentUserId.value
                ? last.receiverNickname
                : last.senderNickname,
            lastMessageContent: previewFromMessage(last),
            lastMessageAt: last.timestamp,
            unreadCount: data.filter(
              (msg) => msg.receiverId === currentUserId.value && !msg.read
            ).length
          });
        } else {
          ensureMeta(partnerId);
        }
      }
    } finally {
      loadingMessages.value = false;
    }
  };

  const sendMessage = async (
    partnerId: number,
    content: string,
    type: 'TEXT' | 'IMAGE' = 'TEXT'
  ) => {
    const trimmed = content.trim();
    if (!trimmed) {
      return;
    }
    if (!userStore.token) {
      throw new Error('Not authenticated');
    }
    if (!connected.value) {
      connect();
    }
    const { data } = await sendChatMessage({
      receiverId: partnerId,
      content: trimmed,
      messageType: type
    });
    insertMessage(data);
  };

  const sendImageMessage = async (partnerId: number, file: File) => {
    const { data } = await uploadChatImage(file);
    await sendMessage(partnerId, data.url, 'IMAGE');
  };

  const markPartnerMessagesRead = async (partnerId: number) => {
    if (!hasToken.value) {
      return;
    }
    if ((conversationMeta.value[partnerId]?.unreadCount ?? 0) === 0) {
      return;
    }
    await markConversationReadApi(partnerId);
    const updatedList = (messageMap.value[partnerId] ?? []).map((msg) => {
      if (msg.senderId === partnerId && !msg.read) {
        return {
          ...msg,
          read: true,
          readAt: new Date().toISOString()
        };
      }
      return msg;
    });
    messageMap.value = {
      ...messageMap.value,
      [partnerId]: updatedList
    };
    if (conversationMeta.value[partnerId]) {
      conversationMeta.value = {
        ...conversationMeta.value,
        [partnerId]: {
          ...conversationMeta.value[partnerId],
          unreadCount: 0
        }
      };
    }
  };

  watch(
    () => userStore.token,
    (token) => {
      if (token) {
        connect();
        refreshConversations();
      } else {
        disconnect(true);
      }
    },
    { immediate: true }
  );

  return {
    connected,
    connecting,
    loadingConversations,
    loadingMessages,
    conversations,
    conversationFor,
    unreadCount,
    connect,
    disconnect,
    refreshConversations,
    loadConversationMessages,
    sendMessage,
    sendImageMessage,
    markPartnerMessagesRead
  };
});
