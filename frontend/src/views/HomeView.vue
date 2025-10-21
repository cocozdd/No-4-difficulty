<template>
  <div class="home-view">
    <section class="hero">
      <h1>Welcome to Campus Market</h1>
      <p>Trade idle items, share campus resources, and keep transactions simple.</p>
      <el-button type="primary" size="large" @click="goGoods">Browse Listings</el-button>
    </section>

    <section class="hot-goods" v-if="featuredAll.length">
      <h2>Latest Picks</h2>
      <el-row :gutter="16">
        <el-col v-for="item in featuredAll" :key="item.id" :xs="24" :sm="12" :md="8">
          <el-card shadow="hover" class="goods-card" @click="goGoodsDetail(item.id)">
            <img
              :src="item.coverImageUrl || getFallbackImageByCategory(item.category)"
              alt=""
              class="goods-cover"
            />
            <div class="goods-content">
              <h3>{{ item.title }}</h3>
              <p class="price">CNY {{ item.price }}</p>
              <p class="stock">Available: {{ item.quantity }}</p>
              <p class="seller">Seller: {{ item.sellerNickname }}</p>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <section class="category-highlights" v-if="categoryHighlights.length">
      <h2>Category Highlights</h2>
      <el-row :gutter="16">
        <el-col
          v-for="highlight in categoryHighlights"
          :key="highlight.category"
          :xs="24"
          :sm="12"
          :md="8"
        >
          <el-card shadow="hover" class="goods-card" @click="goGoodsDetail(highlight.item.id)">
            <div class="category-chip">{{ highlight.category }}</div>
            <img
              :src="highlight.item.coverImageUrl || getFallbackImageByCategory(highlight.category)"
              alt=""
              class="goods-cover"
            />
            <div class="goods-content">
              <h3>{{ highlight.item.title }}</h3>
              <p class="price">CNY {{ highlight.item.price }}</p>
              <p class="stock">Available: {{ highlight.item.quantity }}</p>
              <p class="seller">Seller: {{ highlight.item.sellerNickname }}</p>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <section class="ranking-section">
      <div class="ranking-header">
        <h2>Top Ranking</h2>
        <div class="ranking-tabs">
          <el-button
            v-for="metric in rankingMetrics"
            :key="metric.value"
            size="small"
            :type="metric.value === activeMetric ? 'primary' : 'default'"
            @click="setMetric(metric.value)"
          >
            {{ metric.label }}
          </el-button>
        </div>
      </div>
      <el-empty v-if="!rankingItems.length" description="No ranking data yet" />
      <el-card v-else shadow="never" class="ranking-card">
        <div
          v-for="(item, index) in rankingItems"
          :key="item.goods.id"
          class="ranking-item"
          @click="goGoodsDetail(item.goods.id)"
        >
          <span class="ranking-index">{{ index + 1 }}</span>
          <img
            :src="item.goods.coverImageUrl || getFallbackImageByCategory(item.goods.category)"
            alt=""
            class="ranking-cover"
          />
          <div class="ranking-meta">
            <h3>{{ item.goods.title }}</h3>
            <p class="ranking-score">
              {{ activeMetricLabel }}: {{ item.score.toFixed(0) }}
            </p>
            <p class="ranking-price">CNY {{ item.goods.price }}</p>
          </div>
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useGoodsStore } from '../stores/goodsStore';
import { getFallbackImageByCategory } from '../utils/image';
import { fetchGoodsRanking, fetchHotGoods } from '../apis/goods';
import type { GoodsItem, HotGoodsItem, RankingMetric } from '../apis/goods';

const router = useRouter();
const goodsStore = useGoodsStore();

const FEATURED_LIMIT = 3;
const hotGoods = ref<GoodsItem[]>([]);
const featuredAll = computed(() => {
  if (hotGoods.value.length) {
    return hotGoods.value.slice(0, FEATURED_LIMIT);
  }
  return goodsStore.goods.slice(0, FEATURED_LIMIT);
});

const categoryHighlights = computed(() => {
  const firstByCategory = new Map<string, GoodsItem>();
  goodsStore.goods.forEach((item) => {
    if (!firstByCategory.has(item.category)) {
      firstByCategory.set(item.category, item);
    }
  });
  return Array.from(firstByCategory.entries()).map(([category, item]) => ({
    category,
    item
  }));
});

const loadHotGoods = async () => {
  try {
    const { data } = await fetchHotGoods(FEATURED_LIMIT);
    hotGoods.value = data.map((item: HotGoodsItem) => item.goods);
  } catch {
    hotGoods.value = [];
  }
};

const RANKING_LIMIT = 5;
const rankingMetrics: { value: RankingMetric; label: string }[] = [
  { value: 'orders', label: 'Orders' },
  { value: 'views', label: 'Views' },
  { value: 'carts', label: 'Carts' }
];
const activeMetric = ref<RankingMetric>('orders');
const rankingItems = ref<HotGoodsItem[]>([]);
const activeMetricLabel = computed(
  () => rankingMetrics.find((item) => item.value === activeMetric.value)?.label ?? 'Score'
);

const loadRanking = async (metric: RankingMetric) => {
  try {
    const { data } = await fetchGoodsRanking(metric, RANKING_LIMIT);
    rankingItems.value = data;
  } catch {
    rankingItems.value = [];
  }
};

const setMetric = (metric: RankingMetric) => {
  if (metric !== activeMetric.value) {
    activeMetric.value = metric;
  }
};

onMounted(() => {
  goodsStore.loadGoods();
  loadHotGoods();
  loadRanking(activeMetric.value);
});

watch(activeMetric, (metric) => {
  loadRanking(metric);
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

.hot-goods h2,
.category-highlights h2 {
  margin-bottom: 16px;
}

.goods-card {
  cursor: pointer;
  border-radius: 12px;
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

.stock {
  color: #4b5563;
}

.category-chip {
  display: inline-flex;
  align-items: center;
  background: #e0f2fe;
  color: #0369a1;
  border-radius: 999px;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 8px;
}

.ranking-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ranking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.ranking-tabs {
  display: inline-flex;
  gap: 8px;
}

.ranking-card {
  padding: 0 16px 16px;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
}

.ranking-item:last-child {
  border-bottom: none;
}

.ranking-index {
  font-size: 18px;
  font-weight: 600;
  width: 24px;
  text-align: center;
  color: #1e90ff;
}

.ranking-cover {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 8px;
}

.ranking-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ranking-meta h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2933;
}

.ranking-price {
  color: #4b5563;
  font-weight: 500;
}

.ranking-score {
  font-size: 13px;
  color: #6b7280;
}
</style>
