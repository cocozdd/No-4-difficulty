import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { GoodsFilterParams, GoodsItem, GoodsUpdatePayload, GoodsStatus } from '../apis/goods';
import {
  createGoods as createGoodsApi,
  deleteGoods as deleteGoodsApi,
  fetchGoods,
  fetchGoodsDetail,
  fetchMyGoods,
  fetchPendingGoods,
  reviewGoods as reviewGoodsApi,
  updateGoods as updateGoodsApi
} from '../apis/goods';

export const useGoodsStore = defineStore('goodsStore', () => {
  const goods = ref<GoodsItem[]>([]);
  const myGoods = ref<GoodsItem[]>([]);
  const pendingGoods = ref<GoodsItem[]>([]);
  const loading = ref(false);
  const selectedGoods = ref<GoodsItem | null>(null);
  const pendingLoading = ref(false);

  const loadGoods = async (params?: GoodsFilterParams) => {
    loading.value = true;
    try {
      const { data } = await fetchGoods(params);
      goods.value = data;
    } finally {
      loading.value = false;
    }
  };

  const loadGoodsById = async (id: number) => {
    selectedGoods.value = null;
    const { data } = await fetchGoodsDetail(id);
    selectedGoods.value = data;
  };

  const syncLists = (item: GoodsItem) => {
    if (item.status === 'APPROVED') {
      const exists = goods.value.some((entry) => entry.id === item.id);
      goods.value = exists
        ? goods.value.map((entry) => (entry.id === item.id ? item : entry))
        : [item, ...goods.value];
    } else {
      goods.value = goods.value.filter((entry) => entry.id !== item.id);
    }

    const myIndex = myGoods.value.findIndex((entry) => entry.id === item.id);
    if (myIndex >= 0) {
      myGoods.value = myGoods.value.map((entry) => (entry.id === item.id ? item : entry));
    }

    if (selectedGoods.value?.id === item.id) {
      selectedGoods.value = item;
    }
  };

  const createGoods = async (payload: Parameters<typeof createGoodsApi>[0]) => {
    const { data } = await createGoodsApi(payload);
    if (data.status === 'APPROVED') {
      goods.value = [data, ...goods.value];
    }
    myGoods.value = [data, ...myGoods.value];
    return data;
  };

  const loadMyGoods = async () => {
    const { data } = await fetchMyGoods();
    myGoods.value = data;
  };

  const updateGoods = async (id: number, payload: GoodsUpdatePayload) => {
    const { data } = await updateGoodsApi(id, payload);
    goods.value = data.status === 'APPROVED'
      ? goods.value.map((item) => (item.id === id ? data : item))
      : goods.value.filter((item) => item.id !== id);
    myGoods.value = myGoods.value.map((item) => (item.id === id ? data : item));
    if (selectedGoods.value?.id === id) {
      selectedGoods.value = data;
    }
    return data;
  };

  const loadPendingGoods = async () => {
    pendingLoading.value = true;
    try {
      const { data } = await fetchPendingGoods();
      pendingGoods.value = data;
    } finally {
      pendingLoading.value = false;
    }
  };

  const reviewGoods = async (id: number, status: GoodsStatus) => {
    const { data } = await reviewGoodsApi(id, { status });
    pendingGoods.value = pendingGoods.value.filter((item) => item.id !== id);
    syncLists(data);
    return data;
  };

  const deleteGoods = async (id: number) => {
    await deleteGoodsApi(id);
    goods.value = goods.value.filter((item) => item.id !== id);
    myGoods.value = myGoods.value.filter((item) => item.id !== id);
    if (selectedGoods.value?.id === id) {
      selectedGoods.value = null;
    }
  };

  return {
    goods,
    myGoods,
    pendingGoods,
    loading,
    pendingLoading,
    selectedGoods,
    loadGoods,
    loadGoodsById,
    createGoods,
    loadMyGoods,
    loadPendingGoods,
    reviewGoods,
    updateGoods,
    deleteGoods
  };
});
