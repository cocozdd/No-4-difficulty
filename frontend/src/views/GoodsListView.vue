<template>
  <div class="goods-list-view">
    <el-card class="filter-card">
      <el-form :model="filters" inline @submit.prevent>
        <el-form-item label="Category">
          <el-select v-model="filters.category" placeholder="Filter by category" clearable>
            <el-option label="All" :value="undefined" />
            <el-option label="Electronics" value="Electronics" />
            <el-option label="Books" value="Books" />
            <el-option label="Daily" value="Daily" />
          </el-select>
        </el-form-item>
        <el-form-item label="Price Range">
          <el-input-number v-model="filters.minPrice" :min="0" placeholder="Min" />
          <span class="divider">~</span>
          <el-input-number v-model="filters.maxPrice" :min="0" placeholder="Max" />
        </el-form-item>
        <el-form-item label="Keyword">
          <el-input v-model="filters.keyword" placeholder="Search product" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="applyFilters">Apply</el-button>
          <el-button @click="resetFilters">Reset</el-button>
          <el-button type="success" v-if="isAuthenticated" @click="goMyGoods">
            Manage My Goods
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-skeleton :loading="goodsStore.loading" animated>
      <template #template>
        <el-skeleton-item variant="rect" style="width: 100%; height: 148px; margin-bottom: 16px" />
      </template>
      <el-row :gutter="16" class="goods-grid">
        <el-col v-for="item in goodsStore.goods" :key="item.id" :xs="24" :sm="12" :md="8">
          <el-card class="goods-card" shadow="hover">
            <img
              :src="item.coverImageUrl || getFallbackImageByCategory(item.category)"
              alt=""
              class="goods-cover"
            />
            <div class="goods-body">
              <h3>{{ item.title }}</h3>
              <p class="description">{{ item.description }}</p>
              <div class="meta">
                <span class="price">CNY {{ item.price }}</span>
                <el-button size="small" type="primary" @click="goDetail(item.id)">View detail</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useGoodsStore } from '../stores/goodsStore';
import { useUserStore } from '../stores/userStore';
import { getFallbackImageByCategory } from '../utils/image';

const router = useRouter();
const goodsStore = useGoodsStore();
const userStore = useUserStore();

const filters = reactive({
  category: undefined as string | undefined,
  minPrice: undefined as number | undefined,
  maxPrice: undefined as number | undefined,
  keyword: ''
});

const isAuthenticated = computed(() => userStore.isAuthenticated);

const applyFilters = () => {
  goodsStore.loadGoods({
    category: filters.category,
    minPrice: filters.minPrice,
    maxPrice: filters.maxPrice,
    keyword: filters.keyword
  });
};

const resetFilters = () => {
  filters.category = undefined;
  filters.minPrice = undefined;
  filters.maxPrice = undefined;
  filters.keyword = '';
  goodsStore.loadGoods();
};

const goDetail = (id: number) => router.push(`/goods/${id}`);
const goMyGoods = () => router.push('/goods/mine');

if (!goodsStore.goods.length) {
  goodsStore.loadGoods();
}
</script>

<style scoped>
.goods-list-view {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card {
  border-radius: 16px;
}

.divider {
  margin: 0 8px;
}

.goods-grid {
  margin-top: 8px;
}

.goods-card {
  border-radius: 16px;
  overflow: hidden;
}

.goods-cover {
  width: 100%;
  height: 180px;
  object-fit: cover;
}

.goods-body {
  margin-top: 12px;
}

.description {
  height: 40px;
  color: #6b7280;
  overflow: hidden;
}

.meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  color: #1e90ff;
  font-weight: 600;
}
</style>
