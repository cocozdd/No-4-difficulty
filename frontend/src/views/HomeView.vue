<template>
  <div class="home-view">
    <section class="hero">
      <h1>Welcome to Campus Market</h1>
      <p>Trade idle items, share campus resources, and keep transactions simple.</p>
      <el-button type="primary" size="large" @click="goGoods">Browse Listings</el-button>
    </section>
    <section class="hot-goods">
      <h2>Featured Items</h2>
      <el-row :gutter="16">
        <el-col v-for="item in hotGoods" :key="item.id" :xs="24" :sm="12" :md="8">
          <el-card shadow="hover" class="goods-card" @click="goGoodsDetail(item.id)">
            <img
              :src="item.coverImageUrl || getFallbackImageByCategory(item.category)"
              alt=""
              class="goods-cover"
            />
            <div class="goods-content">
              <h3>{{ item.title }}</h3>
              <p class="price">CNY {{ item.price }}</p>
              <p class="seller">Seller: {{ item.sellerNickname }}</p>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useGoodsStore } from '../stores/goodsStore';
import { getFallbackImageByCategory } from '../utils/image';

const router = useRouter();
const goodsStore = useGoodsStore();

const hotGoods = computed(() => goodsStore.goods.slice(0, 6));

onMounted(() => {
  goodsStore.loadGoods();
});

const goGoods = () => router.push('/goods');
const goGoodsDetail = (id: number) => router.push(`/goods/${id}`);
</script>

<style scoped>
.home-view {
  display: flex;
  flex-direction: column;
  gap: 32px;
  padding: 24px;
}

.hero {
  background: linear-gradient(135deg, #93c5fd 0%, #3b82f6 100%);
  color: #fff;
  padding: 48px 32px;
  border-radius: 16px;
  text-align: center;
}

.hero h1 {
  margin-bottom: 8px;
}

.hot-goods h2 {
  margin-bottom: 16px;
}

.goods-card {
  cursor: pointer;
}

.goods-cover {
  width: 100%;
  height: 180px;
  object-fit: cover;
  border-radius: 8px;
}

.goods-content {
  margin-top: 12px;
}

.price {
  color: #1e90ff;
  font-weight: 600;
}
</style>
