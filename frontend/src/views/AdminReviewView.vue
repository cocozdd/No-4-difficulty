<template>
  <div class="admin-review-view">
    <el-card class="review-card">
      <template #header>
        <div class="header-content">
          <h2>Pending Listings</h2>
          <div class="header-actions">
            <el-button type="primary" text @click="refresh" :loading="goodsStore.pendingLoading">
              Refresh
            </el-button>
          </div>
        </div>
      </template>
      <el-empty
        v-if="!goodsStore.pendingLoading && !goodsStore.pendingGoods.length"
        description="No pending listings 🎉"
      />
      <el-table
        v-else
        :data="goodsStore.pendingGoods"
        v-loading="goodsStore.pendingLoading"
        border
        style="width: 100%"
      >
        <el-table-column prop="title" label="Title" min-width="180" />
        <el-table-column prop="category" label="Category" width="140" />
        <el-table-column prop="price" label="Price (CNY)" width="140">
          <template #default="{ row }"> {{ row.price.toFixed(2) }} </template>
        </el-table-column>
        <el-table-column label="Seller" min-width="160">
          <template #default="{ row }"> {{ row.sellerNickname }} (ID: {{ row.sellerId }}) </template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="Submitted At" min-width="200">
          <template #default="{ row }">
            {{ formatDateTime(row.publishedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="220" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button
                size="small"
                type="success"
                plain
                @click="handleReview(row.id, 'APPROVED')"
                :loading="isRowLoading(row.id, 'APPROVED')"
              >
                Approve
              </el-button>
              <el-button
                size="small"
                type="danger"
                plain
                @click="handleReview(row.id, 'REJECTED')"
                :loading="isRowLoading(row.id, 'REJECTED')"
              >
                Reject
              </el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { useGoodsStore } from '../stores/goodsStore';
import type { GoodsStatus } from '../apis/goods';
import { formatDateTime } from '../utils/date';

const goodsStore = useGoodsStore();
const loadingState = reactive<Record<string, boolean>>({});

const rowKey = (id: number, status: GoodsStatus) => `${id}-${status}`;

const isRowLoading = (id: number, status: GoodsStatus) => loadingState[rowKey(id, status)] === true;

const handleReview = async (id: number, status: GoodsStatus) => {
  const key = rowKey(id, status);
  if (loadingState[key]) {
    return;
  }
  loadingState[key] = true;
  try {
    const result = await goodsStore.reviewGoods(id, status);
    ElMessage.success(
      status === 'APPROVED'
        ? `Listing "${result.title}" approved`
        : `Listing "${result.title}" rejected`
    );
  } catch (error: any) {
    const message = error?.response?.data?.message || 'Failed to process review';
    ElMessage.error(message);
  } finally {
    loadingState[key] = false;
  }
};

const refresh = () => goodsStore.loadPendingGoods();

onMounted(() => {
  goodsStore.loadPendingGoods();
});
</script>

<style scoped>
.admin-review-view {
  padding: 24px;
}

.review-card {
  border-radius: 16px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 12px;
}
</style>
