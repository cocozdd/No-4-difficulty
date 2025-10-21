<template>
  <div class="cart-view">
    <h1>Shopping Cart</h1>
    <el-card>
      <el-skeleton :loading="cartStore.loading" animated>
        <template #template>
          <el-skeleton-item
            variant="rect"
            style="width: 100%; height: 140px; margin-bottom: 16px"
          />
        </template>
        <template #default>
          <div v-if="!cartStore.items.length" class="empty-cart">
            <el-empty description="Your cart is empty">
              <el-button type="primary" @click="goBrowse">Browse goods</el-button>
            </el-empty>
          </div>
          <div v-else class="cart-layout">
            <div class="cart-items">
              <div
                v-for="item in cartStore.items"
                :key="item.id"
                class="cart-item"
              >
                <img
                  :src="
                    item.goodsCoverImageUrl ||
                    getFallbackImageByCategory(item.goodsCategory || 'Daily')
                  "
                  alt=""
                  class="thumbnail"
                />
                <div class="details">
                  <div class="title-row">
                    <h3>{{ item.goodsTitle }}</h3>
                    <el-tag
                      type="danger"
                      v-if="item.goodsSold || item.goodsStatus !== 'APPROVED'"
                    >
                      Unavailable
                    </el-tag>
                  </div>
                  <div class="price">CNY {{ item.goodsPrice ?? '--' }}</div>
                  <div class="stock">
                    In stock:
                    {{ item.goodsQuantityAvailable ?? 0 }}
                  </div>
                  <div class="actions">
                    <el-input-number
                      v-model="quantities[item.id]"
                      :min="1"
                      :max="item.goodsQuantityAvailable ?? 1"
                      :disabled="item.goodsSold || item.goodsStatus !== 'APPROVED'"
                      @change="handleQuantityChange(item)"
                    />
                    <el-button type="text" @click="remove(item.id)">Remove</el-button>
                  </div>
                </div>
              </div>
            </div>
            <div class="cart-summary">
              <h3>Summary</h3>
              <p>Total items: {{ cartStore.totalQuantity }}</p>
              <p>Total amount: CNY {{ cartStore.totalAmount.toFixed(2) }}</p>
              <el-button
                type="warning"
                plain
                @click="purgeSold"
                :disabled="!hasUnavailable"
              >
                Remove unavailable items
              </el-button>
              <el-button type="primary" class="checkout-btn" @click="goOrders">
                Go to orders
              </el-button>
            </div>
          </div>
        </template>
      </el-skeleton>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useCartStore } from '../stores/cartStore';
import { getFallbackImageByCategory } from '../utils/image';

const cartStore = useCartStore();
const router = useRouter();

const quantities = reactive<Record<number, number>>({});

const hasUnavailable = computed(() =>
  cartStore.items.some(
    (item) => item.goodsSold || item.goodsStatus !== 'APPROVED'
  )
);

const syncQuantities = () => {
  const activeIds = new Set<number>();
  cartStore.items.forEach((item) => {
    activeIds.add(item.id);
    quantities[item.id] = item.quantity;
  });
  Object.keys(quantities).forEach((key) => {
    const id = Number(key);
    if (!activeIds.has(id)) {
      Reflect.deleteProperty(quantities, id);
    }
  });
};

const handleQuantityChange = async (item: (typeof cartStore.items)[number]) => {
  const value = quantities[item.id];
  if (!value || value === item.quantity) {
    return;
  }
  try {
    await cartStore.changeQuantity(item.id, { quantity: value });
    ElMessage.success('Quantity updated');
  } catch (error: any) {
    quantities[item.id] = item.quantity;
    const message = error?.response?.data?.message || 'Failed to update quantity';
    ElMessage.error(message);
  }
};

const remove = async (cartItemId: number) => {
  try {
    await cartStore.removeItem(cartItemId);
    Reflect.deleteProperty(quantities, cartItemId);
    ElMessage.success('Removed from cart');
  } catch (error: any) {
    const message = error?.response?.data?.message || 'Failed to remove item';
    ElMessage.error(message);
  }
};

const purgeSold = async () => {
  const removed = await cartStore.purgeSold();
  if (removed > 0) {
    ElMessage.success(`Removed ${removed} unavailable item(s)`);
  } else {
    ElMessage.info('No unavailable items found');
  }
};

const goOrders = () => router.push('/orders');
const goBrowse = () => router.push('/goods');

onMounted(async () => {
  await cartStore.loadCart();
  syncQuantities();
});

watch(
  () => cartStore.items,
  () => {
    syncQuantities();
  },
  { deep: true }
);
</script>

<style scoped>
.cart-view {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cart-layout {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

@media (min-width: 992px) {
  .cart-layout {
    flex-direction: row;
    align-items: flex-start;
  }
}

.cart-items {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cart-item {
  display: flex;
  gap: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 12px;
}

.thumbnail {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 12px;
}

.details {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.price {
  font-weight: 600;
  color: #1e90ff;
}

.stock {
  font-size: 13px;
  color: #6b7280;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cart-summary {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
  min-width: 260px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.checkout-btn {
  width: 100%;
}

.empty-cart {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 240px;
}
</style>
