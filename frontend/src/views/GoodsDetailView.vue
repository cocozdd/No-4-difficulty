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
            <el-tag v-if="goods" :type="statusTagType" size="small">
              {{ statusLabel }}
            </el-tag>
          </div>
          <p class="price">CNY {{ goods.price }}</p>
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
            <el-alert
              :title="ownerAlert.message"
              :type="ownerAlert.type"
              show-icon
              :closable="false"
            />
          </template>
          <template v-else-if="!isSold">
            <div class="actions">
              <el-button type="primary" size="large" @click="placeOrder" :loading="loading">
                Place Order
              </el-button>
              <el-button size="large" @click="goChat" disabled>Contact Seller (coming soon)</el-button>
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
    <el-dialog v-model="editDialogVisible" title="Edit Listing" width="480px" @closed="handleEditDialogClosed">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item label="Price (CNY)" prop="price">
          <el-input-number v-model="editForm.price" :min="0.01" :step="1" />
        </el-form-item>
        <el-form-item label="Description" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="Cover Image URL">
          <el-input v-model="editForm.coverImageUrl" placeholder="Leave empty to keep existing" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">Cancel</el-button>
          <el-button type="primary" :loading="editSubmitting" @click="submitEdit">Save</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
  <el-empty v-else description="Item not found" />
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useGoodsStore } from '../stores/goodsStore';
import { useOrderStore } from '../stores/orderStore';
import { useUserStore } from '../stores/userStore';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { formatDateTime } from '../utils/date';
import { getFallbackImageByCategory } from '../utils/image';
import type { GoodsStatus } from '../apis/goods';

const route = useRoute();
const router = useRouter();
const goodsStore = useGoodsStore();
const orderStore = useOrderStore();
const userStore = useUserStore();
const loading = ref(false);
const editDialogVisible = ref(false);
const editSubmitting = ref(false);
const deleteLoading = ref(false);
const editFormRef = ref<FormInstance>();
const editForm = reactive({
  price: 0,
  description: '',
  coverImageUrl: ''
});
const editRules: FormRules = {
  price: [{ required: true, message: 'Price is required', trigger: 'change' }],
  description: [{ required: true, message: 'Description is required', trigger: 'blur' }]
};

const goodsId = Number(route.params.id);
const goods = computed(() => goodsStore.selectedGoods);
const isOwner = computed(() => goods.value && userStore.userId === goods.value.sellerId);
const isSold = computed(() => goods.value?.sold ?? false);
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

const statusLabel = computed(() => statusLabelMap[status.value]);
const statusTagType = computed(() => statusTagTypeMap[status.value]);

onMounted(() => {
  goodsStore.loadGoodsById(goodsId).catch(() => {
    ElMessage.error('Failed to load item details');
  });
});

watch(
  goods,
  (value) => {
    if (!value) return;
    editForm.price = value.price;
    editForm.description = value.description;
    editForm.coverImageUrl = value.coverImageUrl || '';
  },
  { immediate: true }
);

const placeOrder = async () => {
  if (!userStore.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: route.fullPath } });
    return;
  }
  loading.value = true;
  try {
    await orderStore.submitOrder({ goodsId });
    ElMessage.success('Order created. Continue the flow from the Orders page.');
    router.push('/orders');
  } catch (error: any) {
    const message = error?.response?.data?.message || 'Order creation failed';
    ElMessage.error(message);
  } finally {
    loading.value = false;
  }
};

const goChat = () => {
  ElMessage.info('Chat will be available after the WebSocket module is integrated');
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
  gap: 12px;
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
</style>
