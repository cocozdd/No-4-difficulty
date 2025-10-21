<template>
  <div class="app-shell">
    <el-container>
      <el-header class="app-header">
        <div class="brand">Campus Market</div>
        <div class="header-actions">
          <router-link to="/">Home</router-link>
          <router-link to="/goods">Goods</router-link>
          <router-link to="/orders" v-if="userStore.isAuthenticated">Orders</router-link>
          <router-link to="/goods/mine" v-if="userStore.isAuthenticated">My Goods</router-link>
          <el-badge
            v-if="userStore.isAuthenticated"
            :value="cartCount"
            :hidden="cartCount === 0"
          >
            <router-link to="/cart">Cart</router-link>
          </el-badge>
          <router-link
            to="/admin/review"
            v-if="userStore.isAuthenticated && isAdmin"
          >
            Review Queue
          </router-link>
          <el-badge
            v-if="userStore.isAuthenticated"
            :value="totalUnread"
            :hidden="totalUnread === 0"
            class="message-entry"
          >
            <el-button text type="primary" @click="openChat">
              <el-icon><chat-dot-round /></el-icon>
            </el-button>
          </el-badge>
          <router-link to="/login" v-if="!userStore.isAuthenticated">Login</router-link>
          <el-dropdown v-else>
            <span class="el-dropdown-link">
              {{ userStore.nickname }}<el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goProfile">Profile</el-dropdown-item>
                <el-dropdown-item @click="goMyGoods">My Goods</el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" @click="goReview">Review Queue</el-dropdown-item>
                <el-dropdown-item divided @click="logout">Logout</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
    <chat-center v-model="chatVisible" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { ArrowDown, ChatDotRound } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import { useUserStore } from './stores/userStore';
import { useChatStore } from './stores/chatStore';
import { useCartStore } from './stores/cartStore';
import ChatCenter from './components/ChatCenter.vue';

const router = useRouter();
const userStore = useUserStore();
const chatStore = useChatStore();
const cartStore = useCartStore();
const isAdmin = computed(() => userStore.role === 'ADMIN');
const chatVisible = ref(false);

const totalUnread = computed(() =>
  chatStore.conversations.reduce((count, item) => count + item.unreadCount, 0)
);

const cartCount = computed(() => cartStore.totalQuantity);

const openChat = () => {
  chatVisible.value = true;
};

const logout = () => {
  userStore.logout();
  router.push('/login');
};

const goProfile = () => {
  router.push('/profile');
};

const goMyGoods = () => {
  router.push('/goods/mine');
};

const goReview = () => {
  router.push('/admin/review');
};

onMounted(() => {
  if (userStore.isAuthenticated) {
    cartStore.loadCart().catch(() => {});
  }
});

watch(
  () => userStore.isAuthenticated,
  (loggedIn) => {
    if (loggedIn) {
      cartStore.loadCart().catch(() => {});
    } else {
      cartStore.items = [];
    }
  }
);
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #1e90ff;
  color: #fff;
  padding: 0 24px;
  height: 64px;
}

.brand {
  font-size: 20px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-actions a {
  color: #fff;
}

.header-actions :deep(.el-badge__content) {
  background-color: #ef4444;
  border: none;
}

.message-entry {
  display: inline-flex;
  align-items: center;
}

.message-entry .el-button {
  color: #fff;
  padding: 0;
  height: auto;
}
</style>
