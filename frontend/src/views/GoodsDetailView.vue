<template>
  <div class="detail-view" v-if="goods">
    <el-row :gutter="24">
      <el-col :span="14">
        <img :src="goods.coverImageUrl || getFallbackImageByCategory(goods.category)" alt="" class="detail-cover" />
      </el-col>
      <el-col :span="10">
        <div class="detail-content">
          <div class="title-row">
            <h1>{{ goods.title }}</h1>
            <el-tag :type="statusTagType" size="small">
              {{ statusLabel }}
            </el-tag>
          </div>
          <p class="price">CNY {{ goods.price }}</p>
          <p class="stock">Available: {{ goods.quantity }}</p>
          <p>{{ goods.description }}</p>
          <div class="seller-card">
            <p>Seller: {{ goods.sellerNickname }}</p>
            <p>Published: {{ formatDateTime(goods.publishedAt) }}</p>
          </div>
          <template v-if="isOwner">
            <div class="owner-actions">
              <el-button type="primary" size="large" @click="openEditDialog" :disabled="isSold">
                Edit Listing
              </el-button>
              <el-button
                type="danger"
                size="large"
                :loading="deleteLoading"
                @click="confirmDelete"
                :disabled="isSold"
              >
                Delete Listing
              </el-button>
            </div>
            <el-alert :title="ownerAlert.message" :type="ownerAlert.type" show-icon :closable="false" />
          </template>
          <template v-else-if="!isSold">
            <div class="actions">
              <div class="cart-actions" v-if="canAddToCart">
                <span>Quantity</span>
                <el-input-number
                  v-model="addQuantity"
                  :min="1"
                  :max="availableQuantity"
                  :disabled="addingToCart"
                />
                <el-button
                  type="primary"
                  plain
                  size="large"
                  :loading="addingToCart"
                  @click="handleAddToCart"
                >
                  Add to Cart
                </el-button>
              </div>
              <div class="primary-actions">
                <el-button type="primary" size="large" @click="placeOrder" :loading="loading">
                  Place Order
                </el-button>
                <el-button size="large" @click="goChat">
                  Contact Seller
                </el-button>
              </div>
            </div>
          </template>
          <el-alert
            v-else
            title="This item has been sold and is no longer available."
            type="warning"
            show-icon
            :closable="false"
          />
        </div>
      </el-col>
    </el-row>

    <el-dialog v-model="editDialogVisible" title="Edit Listing" width="520px" @closed="handleEditDialogClosed">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item label="Price (CNY)" prop="price">
          <el-input-number v-model="editForm.price" :min="0.01" :step="1" />
        </el-form-item>
        <el-form-item label="Quantity" prop="quantity">
          <el-input-number v-model="editForm.quantity" :min="1" :step="1" />
        </el-form-item>
        <el-form-item label="Description" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="Cover Image">
          <div class="upload-row">
            <el-upload
              class="cover-upload"
              :show-file-list="false"
              :auto-upload="false"
              accept="image/*"
              :before-upload="beforeGoodsImageUpload"
              :on-change="handleGoodsImageChange"
              :disabled="goodsImageUploading"
            >
              <el-button type="primary" :loading="goodsImageUploading">
                <template #icon>
                  <el-icon><PictureFilled /></el-icon>
                </template>
                Upload Image
              </el-button>
            </el-upload>
            <el-image
              v-if="editForm.coverImageUrl"
              :src="editForm.coverImageUrl"
              fit="cover"
              class="cover-preview"
            />
            <el-button v-if="editForm.coverImageUrl" text type="danger" @click="removeGoodsImage">
              Remove
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">Cancel</el-button>
          <el-button type="primary" :loading="editSubmitting" @click="submitEdit">Save</el-button>
        </span>
      </template>
    </el-dialog>

    <el-drawer
      v-model="chatDrawerVisible"
      :title="`Chat with ${chatPartnerName || 'Seller'}`"
      size="30%"
      append-to-body
      @close="handleChatClosed"
    >
      <div class="chat-drawer">
        <div v-if="chatStore.connecting" class="chat-status">Connecting...</div>
        <div v-else-if="!chatStore.connected" class="chat-status chat-status--offline">
          Not connected. Messages will appear once the chat service reconnects.
        </div>
        <div class="chat-body" ref="chatBodyRef">
          <div v-if="!chatMessages.length" class="chat-empty">
            Start a conversation with {{ chatPartnerName || 'the seller' }}.
          </div>
          <div
            v-for="message in chatMessages"
            :key="message.id"
            :class="['chat-message', { outgoing: message.senderId === currentUserId }]"
          >
            <div class="chat-meta">
              <span class="nickname">{{ message.senderId === currentUserId ? 'You' : message.senderNickname }}</span>
              <span class="timestamp">{{ formatChatTimestamp(message.timestamp) }}</span>
            </div>
            <div class="chat-content-row">
              <span
                v-if="message.senderId === chatPartnerId && !message.read"
                class="chat-unread-dot"
              />
              <div
                v-if="message.type === 'IMAGE'"
                class="chat-bubble chat-bubble-image"
              >
                <el-image
                  :src="resolveChatMediaUrl(message.content)"
                  fit="cover"
                  :preview-src-list="[resolveChatMediaUrl(message.content)]"
                  @load="scrollChatToBottom"
                />
              </div>
              <div v-else class="chat-bubble">
                {{ message.content }}
              </div>
            </div>
          </div>
        </div>
        <div class="chat-input">
          <el-input
            v-model="chatInput"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 4 }"
            placeholder="Type a message"
            @keyup.enter.exact.prevent="sendChat"
          />
          <div class="chat-input-actions">
            <el-upload
              v-if="chatPartnerId"
              class="chat-upload"
              :show-file-list="false"
              :auto-upload="false"
              accept="image/*"
              :before-upload="beforeChatImageUpload"
              :on-change="handleChatImageChange"
              :disabled="chatImageUploading"
            >
              <el-button
                text
                type="primary"
                :disabled="chatImageUploading"
                :loading="chatImageUploading"
              >
                <el-icon><PictureFilled /></el-icon>
              </el-button>
            </el-upload>
            <el-button type="primary" :loading="loading" @click="sendChat" :disabled="!canSendChat">
              Send
            </el-button>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
  <el-empty v-else description="Item not found" />
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useGoodsStore } from '../stores/goodsStore';
import { useOrderStore } from '../stores/orderStore';
import { useChatStore } from '../stores/chatStore';
import { useCartStore } from '../stores/cartStore';
import { useUserStore } from '../stores/userStore';
import { ElMessage, ElMessageBox, type UploadFile, type UploadProps } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { PictureFilled } from '@element-plus/icons-vue';
import { formatDateTime } from '../utils/date';
import { recordGoodsView } from '../apis/goods';
import { getFallbackImageByCategory } from '../utils/image';
import type { GoodsStatus } from '../apis/goods';

const route = useRoute();
const router = useRouter();
const goodsStore = useGoodsStore();
const orderStore = useOrderStore();
const chatStore = useChatStore();
const userStore = useUserStore();
const cartStore = useCartStore();

const loading = ref(false);
const editDialogVisible = ref(false);
const editSubmitting = ref(false);
const deleteLoading = ref(false);
const goodsImageUploading = ref(false);
const editFormRef = ref<FormInstance>();
const editForm = reactive({
  price: 0,
  description: '',
  quantity: 1,
  coverImageUrl: ''
});

const editRules: FormRules = {
  price: [{ required: true, message: 'Price is required', trigger: 'change' }],
  quantity: [{ required: true, message: 'Quantity is required', trigger: 'change' }],
  description: [{ required: true, message: 'Description is required', trigger: 'blur' }]
};

const chatDrawerVisible = ref(false);
const chatPartnerId = ref<number | null>(null);
const chatPartnerName = ref('');
const chatInput = ref('');
const chatBodyRef = ref<HTMLElement | null>(null);
const chatImageUploading = ref(false);
const addQuantity = ref(1);
const addingToCart = ref(false);

const goodsId = Number(route.params.id);
const goods = computed(() => goodsStore.selectedGoods);
const currentUserId = computed(() => userStore.userId ?? 0);
const isOwner = computed(() => goods.value && userStore.userId === goods.value.sellerId);
const isSold = computed(() => {
  const soldFlag = goods.value?.sold ?? false;
  const remaining = goods.value?.quantity ?? 0;
  return soldFlag || remaining <= 0;
});
const status = computed<GoodsStatus>(() => goods.value?.status ?? 'PENDING_REVIEW');
const isPending = computed(() => status.value === 'PENDING_REVIEW');
const isRejected = computed(() => status.value === 'REJECTED');

const statusLabelMap: Record<GoodsStatus, string> = {
  PENDING_REVIEW: 'Pending Review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected'
};

const statusTagTypeMap: Record<GoodsStatus, 'info' | 'success' | 'warning' | 'danger'> = {
  PENDING_REVIEW: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger'
};

const statusLabel = computed(() => statusLabelMap[status.value]);
const statusTagType = computed(() => statusTagTypeMap[status.value]);
const availableQuantity = computed(() => goods.value?.quantity ?? 0);
const canAddToCart = computed(() => {
  if (!userStore.isAuthenticated) return false;
  if (isOwner.value) return false;
  if (!goods.value) return false;
  if (status.value !== 'APPROVED') return false;
  return availableQuantity.value > 0 && !isSold.value;
});

const ownerAlert = computed(() => {
  if (isSold.value) {
    return {
      type: 'warning' as const,
      message: 'This item has been sold and can no longer be modified or deleted.'
    };
  }
  if (isPending.value) {
    return {
      type: 'warning' as const,
      message: 'Listing is awaiting admin review. Buyers cannot see it yet.'
    };
  }
  if (isRejected.value) {
    return {
      type: 'error' as const,
      message: 'Listing was rejected. Update the details and resubmit for review.'
    };
  }
  return {
    type: 'success' as const,
    message: 'Listing is live. You can update or remove it here.'
  };
});

const chatMessages = computed(() => {
  if (chatPartnerId.value == null) {
    return [];
  }
  return chatStore.conversationFor(chatPartnerId.value);
});

const resolveChatMediaUrl = (raw: string) => {
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

const canSendChat = computed(() => Boolean(chatInput.value.trim()) && chatPartnerId.value != null);

const formatChatTimestamp = (value: string) => formatDateTime(value);

const scrollChatToBottom = () => {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight;
    }
  });
};

onMounted(() => {
  goodsStore
    .loadGoodsById(goodsId)
    .then(() => {
      recordGoodsView(goodsId).catch(() => {});
    })
    .catch(() => {
      ElMessage.error('Failed to load item details');
    });
});

watch(availableQuantity, (value) => {
  if (value <= 0) {
    addQuantity.value = 1;
    return;
  }
  if (addQuantity.value > value) {
    addQuantity.value = value;
  }
});

watch(
  () => goods.value?.id,
  () => {
    addQuantity.value = 1;
  }
);

const handleAddToCart = async () => {
  if (!goods.value) {
    return;
  }
  if (!userStore.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: route.fullPath } });
    return;
  }
  if (!canAddToCart.value) {
    return;
  }
  addingToCart.value = true;
  try {
    await cartStore.addToCart({
      goodsId: goods.value.id,
      quantity: addQuantity.value
    });
    ElMessage.success('Added to cart');
    cartStore.loadCart().catch(() => {});
  } catch (error: any) {
    const message = error?.response?.data?.message || 'Failed to add to cart';
    ElMessage.error(message);
  } finally {
    addingToCart.value = false;
  }
};

watch(
  goods,
  (value) => {
    if (!value) return;
    editForm.price = value.price;
    editForm.description = value.description;
    editForm.quantity = value.quantity;
    editForm.coverImageUrl = value.coverImageUrl || '';
  },
  { immediate: true }
);

watch(
  () => chatMessages.value.length,
  (length, previousLength) => {
    if (!chatDrawerVisible.value) {
      return;
    }
    if (length !== previousLength) {
      scrollChatToBottom();
      if (chatPartnerId.value != null) {
        chatStore.markPartnerMessagesRead(chatPartnerId.value).catch(() => {});
      }
    }
  }
);

watch(chatDrawerVisible, (open) => {
  if (open) {
    scrollChatToBottom();
    if (chatPartnerId.value != null) {
      chatStore.markPartnerMessagesRead(chatPartnerId.value).catch(() => {});
    }
  } else {
    chatInput.value = '';
  }
});

const placeOrder = async () => {
  if (!userStore.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: route.fullPath } });
    return;
  }
  loading.value = true;
  try {
    await orderStore.submitOrder({ goodsId });
    ElMessage.success('Order created. Continue from the Orders page.');
    router.push('/orders');
  } catch (error: any) {
    const message = error?.response?.data?.message || 'Order creation failed';
    ElMessage.error(message);
  } finally {
    loading.value = false;
  }
};

const goChat = async () => {
  if (!goods.value) return;
  if (!userStore.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: route.fullPath } });
    return;
  }
  if (goods.value.sellerId === userStore.userId) {
    ElMessage.info('You are the seller of this listing.');
    return;
  }
  chatPartnerId.value = goods.value.sellerId;
  chatPartnerName.value = goods.value.sellerNickname;
  chatDrawerVisible.value = true;
  await chatStore.refreshConversations().catch(() => {});
  await chatStore.loadConversationMessages(goods.value.sellerId);
  await chatStore.markPartnerMessagesRead(goods.value.sellerId).catch(() => {});
  scrollChatToBottom();
};

const sendChat = async () => {
  if (!canSendChat.value || chatPartnerId.value == null) {
    return;
  }
  const message = chatInput.value.trim();
  if (!message) {
    return;
  }
  try {
    await chatStore.sendMessage(chatPartnerId.value, message, 'TEXT');
    chatInput.value = '';
    scrollChatToBottom();
  } catch (error: any) {
    const errMsg = error?.message || 'Failed to send message';
    ElMessage.error(errMsg);
  }
};

const CHAT_MAX_IMAGE_SIZE = 5 * 1024 * 1024;

const beforeChatImageUpload: UploadProps['beforeUpload'] = () => false;

const handleChatImageChange: UploadProps['onChange'] = async (uploadFile) => {
  const raw = uploadFile.raw;
  if (!raw) {
    return;
  }
  if (!raw.type.startsWith('image/')) {
    ElMessage.error('Only image files are allowed');
    return;
  }
  if (raw.size > CHAT_MAX_IMAGE_SIZE) {
    ElMessage.error('Image size cannot exceed 5MB');
    return;
  }
  if (chatPartnerId.value == null) {
    ElMessage.warning('Open a chat before sending images');
    return;
  }
  chatImageUploading.value = true;
  try {
    await chatStore.sendImageMessage(chatPartnerId.value, raw);
    scrollChatToBottom();
  } catch (error: any) {
    const errMsg = error?.message || 'Failed to send image';
    ElMessage.error(errMsg);
  } finally {
    chatImageUploading.value = false;
  }
};

const handleChatClosed = () => {
  chatPartnerId.value = null;
  chatPartnerName.value = '';
  chatInput.value = '';
};

const beforeGoodsImageUpload: UploadProps['beforeUpload'] = () => false;

const handleGoodsImageChange: UploadProps['onChange'] = async (uploadFile) => {
  const raw = uploadFile.raw;
  if (!raw) {
    return;
  }
  if (!raw.type.startsWith('image/')) {
    ElMessage.error('Only image files are allowed');
    return;
  }
  if (raw.size > CHAT_MAX_IMAGE_SIZE) {
    ElMessage.error('Image size cannot exceed 5MB');
    return;
  }
  goodsImageUploading.value = true;
  try {
    const { uploadGoodsImage } = await import('../apis/upload');
    const { data } = await uploadGoodsImage(raw);
    editForm.coverImageUrl = data.url;
    ElMessage.success('Image uploaded');
  } catch (error: any) {
    const message = error?.response?.data?.message || 'Failed to upload image';
    ElMessage.error(message);
  } finally {
    goodsImageUploading.value = false;
  }
};

const removeGoodsImage = () => {
  editForm.coverImageUrl = '';
};

const openEditDialog = () => {
  if (isSold.value) {
    ElMessage.warning('Sold listings cannot be edited');
    return;
  }
  if (!goods.value) {
    return;
  }
  editDialogVisible.value = true;
};

const submitEdit = () => {
  if (!goods.value) return;
  editFormRef.value?.validate(async (valid) => {
    if (!valid) return;
    editSubmitting.value = true;
    try {
      const updated = await goodsStore.updateGoods(goodsId, {
        price: editForm.price,
        description: editForm.description,
        quantity: editForm.quantity,
        coverImageUrl: editForm.coverImageUrl?.trim() || undefined
      });
      const message =
        updated.status === 'APPROVED'
          ? 'Listing updated'
          : 'Listing updated and resubmitted for review';
      ElMessage.success(message);
      editDialogVisible.value = false;
    } catch (error: any) {
      const message = error?.response?.data?.message || 'Failed to update listing';
      ElMessage.error(message);
    } finally {
      editSubmitting.value = false;
    }
  });
};

const confirmDelete = async () => {
  if (!goods.value) return;
  if (isSold.value) {
    ElMessage.warning('Sold listings cannot be deleted');
    return;
  }
  try {
    await ElMessageBox.confirm(
      'Delete this listing? This action cannot be undone.',
      'Confirm Deletion',
      {
        type: 'warning',
        confirmButtonText: 'Delete',
        cancelButtonText: 'Cancel'
      }
    );
  } catch {
    return;
  }
  deleteLoading.value = true;
  try {
    await goodsStore.deleteGoods(goodsId);
    ElMessage.success('Listing deleted');
    router.push('/goods/mine');
  } catch (error: any) {
    const message = error?.response?.data?.message || 'Failed to delete listing';
    ElMessage.error(message);
  } finally {
    deleteLoading.value = false;
  }
};

const handleEditDialogClosed = () => {
  editFormRef.value?.clearValidate();
};
</script>

<style scoped>
.detail-view {
  padding: 32px;
}

.detail-cover {
  width: 100%;
  border-radius: 16px;
  object-fit: cover;
  min-height: 360px;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.price {
  font-size: 24px;
  font-weight: 600;
  color: #1e90ff;
}

.stock {
  color: #4b5563;
  font-weight: 500;
}

.seller-card {
  padding: 16px;
  border-radius: 12px;
  background: #f5f9ff;
  color: #1f2933;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cart-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.primary-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.owner-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.cover-preview {
  width: 96px;
  height: 96px;
  border-radius: 12px;
  object-fit: cover;
}

.chat-drawer {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}

.chat-status {
  font-size: 12px;
  color: #2563eb;
}

.chat-status--offline {
  color: #ef4444;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 8px;
}

.chat-empty {
  text-align: center;
  color: #6b7280;
  margin-top: 32px;
}

.chat-message {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-self: flex-start;
  max-width: 80%;
}

.chat-message.outgoing {
  align-self: flex-end;
}

.chat-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #6b7280;
}

.chat-message.outgoing .chat-meta {
  justify-content: flex-end;
  gap: 8px;
  color: rgba(255, 255, 255, 0.85);
}

.chat-content-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.chat-message.outgoing .chat-content-row {
  justify-content: flex-end;
}

.chat-bubble {
  padding: 12px 14px;
  border-radius: 16px;
  background: #f1f5f9;
  color: #1f2933;
  white-space: pre-wrap;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.1);
}

.chat-message.outgoing .chat-bubble {
  background: #1e90ff;
  color: #fff;
}

.chat-bubble-image {
  padding: 6px;
  border-radius: 16px;
  background: #f1f5f9;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08);
}

.chat-message.outgoing .chat-bubble-image {
  background: rgba(30, 144, 255, 0.15);
}

.chat-bubble-image :deep(.el-image) {
  display: block;
  max-width: 220px;
  border-radius: 12px;
}

.chat-bubble-image :deep(img) {
  border-radius: 12px;
}

.chat-unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  margin-top: 8px;
  flex-shrink: 0;
}

.chat-input {
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-top: 1px solid #e5e7eb;
  padding-top: 12px;
}

.chat-input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.chat-upload {
  display: inline-flex;
}

.chat-upload :deep(.el-button) {
  padding: 0;
  height: auto;
}
</style>
