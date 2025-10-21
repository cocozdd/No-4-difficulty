<template>
  <el-drawer
    v-model="visible"
    title="Messages"
    size="50%"
    append-to-body
    :destroy-on-close="false"
    @open="handleOpen"
    @close="handleClose"
  >
    <div class="chat-center">
      <aside class="conversation-pane">
        <div class="pane-header">
          <span>Conversations</span>
          <el-button
            type="primary"
            text
            size="small"
            :loading="chatStore.loadingConversations"
            @click="chatStore.refreshConversations"
          >
            Refresh
          </el-button>
        </div>

        <el-scrollbar class="conversation-scroll">
          <el-empty v-if="!chatStore.conversations.length" description="No conversations yet" />
          <div
            v-for="conversation in chatStore.conversations"
            :key="conversation.partnerId"
            :class="['conversation-item', { active: conversation.partnerId === activePartnerId }]"
            @click="selectConversation(conversation.partnerId)"
          >
            <div class="conversation-title">
              <span class="name">{{ conversation.partnerNickname || 'Unknown' }}</span>
              <span class="time">{{ formatDateTime(conversation.lastMessageAt) }}</span>
            </div>
            <div class="conversation-preview">
              <el-badge
                v-if="conversation.unreadCount > 0"
                :value="conversation.unreadCount"
                class="unread-badge"
              >
                <span class="preview-text">{{ conversation.lastMessageContent }}</span>
              </el-badge>
              <span v-else class="preview-text">{{ conversation.lastMessageContent }}</span>
            </div>
          </div>
        </el-scrollbar>
      </aside>

      <section class="message-pane" :class="{ empty: !activePartnerId }">
        <div v-if="!activePartnerId" class="message-placeholder">
          <el-empty description="Select a conversation to view messages" />
        </div>
        <div v-else class="message-content">
          <header class="message-header">
            <h3>{{ activePartnerName }}</h3>
          </header>

          <el-scrollbar ref="messageScrollbar" class="message-scroll" :noresize="true">
            <div class="message-list" v-loading="chatStore.loadingMessages">
              <div
                v-for="message in activeMessages"
                :key="message.id"
                :class="['message-item', { outgoing: message.senderId === currentUserId }]"
              >
                <div class="message-meta">
                  <span class="nickname">
                    {{ message.senderId === currentUserId ? 'You' : message.senderNickname }}
                  </span>
                  <span class="timestamp">{{ formatDateTime(message.timestamp) }}</span>
                </div>
                <div class="message-body">
                  <span v-if="message.senderId !== currentUserId && !message.read" class="unread-dot" />
                  <div v-if="message.type === 'IMAGE'" class="bubble bubble-image">
                    <el-image
                      :src="resolveMediaUrl(message.content)"
                      fit="cover"
                      :preview-src-list="[resolveMediaUrl(message.content)]"
                      @load="scrollToBottom"
                    />
                  </div>
                  <div v-else class="bubble">
                    {{ message.content }}
                  </div>
                </div>
              </div>
              <el-empty
                v-if="!activeMessages.length && !chatStore.loadingMessages"
                description="No messages yet"
              />
            </div>
          </el-scrollbar>

          <footer class="message-input">
            <el-input
              v-model="messageInput"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 4 }"
              placeholder="Type a message"
              @keyup.enter.exact.prevent="handleSend"
            />
            <div class="input-actions">
              <el-upload
                v-if="activePartnerId"
                class="upload-trigger"
                :show-file-list="false"
                :auto-upload="false"
                accept="image/*"
                :before-upload="() => false"
                :on-change="handleImageChange"
                :disabled="uploadingImage"
              >
                <el-button
                  text
                  type="primary"
                  :loading="uploadingImage"
                  :disabled="uploadingImage"
                >
                  <el-icon><PictureFilled /></el-icon>
                </el-button>
              </el-upload>
              <el-button type="primary" :disabled="!canSend || uploadingImage" @click="handleSend">
                Send
              </el-button>
            </div>
          </footer>
        </div>
      </section>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';
import { ElMessage, type UploadFile, type UploadProps } from 'element-plus';
import { PictureFilled } from '@element-plus/icons-vue';
import { useChatStore } from '../stores/chatStore';
import { useUserStore } from '../stores/userStore';
import { formatDateTime } from '../utils/date';

const MAX_IMAGE_SIZE = 5 * 1024 * 1024;

const props = defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});

const chatStore = useChatStore();
const userStore = useUserStore();

const activePartnerId = ref<number | null>(null);
const messageInput = ref('');
const uploadingImage = ref(false);
const messageScrollbar = ref();

const currentUserId = computed(() => userStore.userId ?? 0);

const activePartnerName = computed(() => {
  if (activePartnerId.value == null) {
    return '';
  }
  const conversation = chatStore.conversations.find(
    (item) => item.partnerId === activePartnerId.value
  );
  return conversation?.partnerNickname || '';
});

const activeMessages = computed(() => {
  if (activePartnerId.value == null) {
    return [];
  }
  return chatStore.conversationFor(activePartnerId.value);
});

const canSend = computed(
  () => Boolean(messageInput.value.trim()) && activePartnerId.value != null
);

const resolveMediaUrl = (raw: string) => {
  if (!raw) {
    return raw;
  }
  try {
    const url = new URL(raw, window.location.origin);
    if (url.hostname === 'localhost' && url.port === '9000') {
      if (url.protocol === 'https:') {
        url.protocol = 'http:';
        return url.toString();
      }
      if (!url.protocol) {
        url.protocol = 'http:';
        return url.toString();
      }
    }
    return url.toString();
  } catch {
    if (raw.startsWith('//localhost:9000')) {
      return `http:${raw}`;
    }
    if (raw.startsWith('https://localhost:9000')) {
      return raw.replace('https://', 'http://');
    }
    return raw;
  }
};

const scrollToBottom = () => {
  nextTick(() => {
    const scrollbar = messageScrollbar.value as
      | { wrapRef?: HTMLElement; update?: () => void }
      | undefined;
    if (scrollbar?.wrapRef) {
      scrollbar.update?.();
      scrollbar.wrapRef.scrollTop = scrollbar.wrapRef.scrollHeight;
    }
  });
};

const selectConversation = async (partnerId: number) => {
  if (activePartnerId.value === partnerId) {
    await chatStore.markPartnerMessagesRead(partnerId).catch(() => {});
    return;
  }
  activePartnerId.value = partnerId;
  await chatStore.loadConversationMessages(partnerId);
  await chatStore.markPartnerMessagesRead(partnerId).catch(() => {});
  scrollToBottom();
};

const handleOpen = async () => {
  try {
    await chatStore.refreshConversations();
    if (chatStore.conversations.length && !activePartnerId.value) {
      await selectConversation(chatStore.conversations[0].partnerId);
    }
  } catch (error) {
    ElMessage.error('Failed to load conversations');
  }
};

const handleClose = () => {
  activePartnerId.value = null;
  messageInput.value = '';
  uploadingImage.value = false;
};

const handleSend = async () => {
  if (!canSend.value || activePartnerId.value == null) {
    return;
  }
  const payload = messageInput.value.trim();
  if (!payload) {
    return;
  }
  try {
    await chatStore.sendMessage(activePartnerId.value, payload, 'TEXT');
    messageInput.value = '';
    scrollToBottom();
  } catch (error: any) {
    const message = error?.message || 'Failed to send message';
    ElMessage.error(message);
  }
};

const handleImageChange: UploadProps['onChange'] = async (uploadFile: UploadFile) => {
  const raw = uploadFile.raw;
  if (!raw) {
    return;
  }
  if (!raw.type.startsWith('image/')) {
    ElMessage.error('Only image files are allowed');
    return;
  }
  if (raw.size > MAX_IMAGE_SIZE) {
    ElMessage.error('Image size cannot exceed 5MB');
    return;
  }
  if (activePartnerId.value == null) {
    ElMessage.warning('Select a conversation before sending images');
    return;
  }

  uploadingImage.value = true;
  try {
    await chatStore.sendImageMessage(activePartnerId.value, raw);
    scrollToBottom();
  } catch (error: any) {
    console.error('[chat] Failed to send image', error);
    const message = error?.message || 'Failed to send image';
    ElMessage.error(message);
  } finally {
    uploadingImage.value = false;
  }
};

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      handleClose();
      return;
    }
    if (chatStore.conversations.length === 0) {
      chatStore.refreshConversations().catch(() => {});
    }
  }
);

watch(
  () => activeMessages.value.length,
  (length, previous) => {
    if (!visible.value) {
      return;
    }
    if (length !== previous) {
      scrollToBottom();
      if (activePartnerId.value != null) {
        chatStore.markPartnerMessagesRead(activePartnerId.value).catch(() => {});
      }
    }
  }
);
</script>

<style scoped>
.chat-center {
  display: flex;
  height: 100%;
  gap: 16px;
}

.conversation-pane {
  width: 240px;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  padding-right: 8px;
}

.pane-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  font-weight: 600;
}

.conversation-scroll {
  flex: 1;
}

.conversation-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 6px;
  transition: background 0.2s ease;
}

.conversation-item:hover {
  background: #f3f4f6;
}

.conversation-item.active {
  background: #e0f2fe;
}

.conversation-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #1f2937;
}

.conversation-title .name {
  font-weight: 600;
}

.conversation-title .time {
  font-size: 12px;
  color: #6b7280;
}

.conversation-preview {
  font-size: 12px;
  color: #6b7280;
}

.unread-badge {
  width: 100%;
}

.preview-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.message-pane.empty {
  justify-content: center;
}

.message-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.message-header {
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.message-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.message-scroll {
  flex: 1;
  padding: 16px 8px 16px 0;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  max-width: 70%;
  align-self: flex-start;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.message-item.outgoing {
  align-self: flex-end;
}

.message-meta {
  font-size: 12px;
  color: #6b7280;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.message-item.outgoing .message-meta {
  justify-content: flex-end;
}

.message-body {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.message-item.outgoing .message-body {
  justify-content: flex-end;
}

.bubble {
  padding: 12px 14px;
  border-radius: 16px;
  background: #f1f5f9;
  color: #1f2933;
  white-space: pre-wrap;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.1);
}

.message-item.outgoing .bubble {
  background: #1e90ff;
  color: #fff;
}

.bubble-image {
  padding: 6px;
  border-radius: 16px;
  background: #f1f5f9;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08);
}

.message-item.outgoing .bubble-image {
  background: rgba(30, 144, 255, 0.15);
}

.bubble-image :deep(.el-image) {
  display: block;
  max-width: 220px;
  border-radius: 12px;
}

.bubble-image :deep(img) {
  border-radius: 12px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  margin-top: 10px;
  flex-shrink: 0;
}

.message-input {
  border-top: 1px solid #e5e7eb;
  padding-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.upload-trigger {
  display: inline-flex;
}

.upload-trigger :deep(.el-button) {
  padding: 0;
  height: auto;
}

.message-placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  flex: 1;
}
</style>
