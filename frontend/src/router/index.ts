import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import { useUserStore } from '../stores/userStore';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'home',
    component: () => import('../views/HomeView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/goods',
    name: 'goods',
    component: () => import('../views/GoodsListView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/goods/:id',
    name: 'goods-detail',
    component: () => import('../views/GoodsDetailView.vue'),
    props: true,
    meta: { requiresAuth: false }
  },
  {
    path: '/goods/mine',
    name: 'goods-mine',
    component: () => import('../views/MyGoodsView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/review',
    name: 'admin-review',
    component: () => import('../views/AdminReviewView.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('../views/OrdersView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/ProfileView.vue'),
    meta: { requiresAuth: true }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore();

  if (userStore.isAuthenticated && !userStore.role) {
    try {
      await userStore.loadProfile();
    } catch {
      userStore.logout();
    }
  }

  if (to.meta.requiresAuth && !userStore.isAuthenticated) {
    next({ name: 'login', query: { redirect: to.fullPath } });
    return;
  }

  if (to.meta.requiresAdmin && userStore.role !== 'ADMIN') {
    next({ name: 'home' });
    return;
  }

  next();
});

export default router;
