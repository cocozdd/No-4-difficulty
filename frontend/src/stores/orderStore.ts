import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { OrderCreatePayload, OrderItem, OrderUpdateStatusPayload } from '../apis/orders';
import { createOrder, listOrders, updateOrderStatus } from '../apis/orders';
import { useGoodsStore } from './goodsStore';

export const useOrderStore = defineStore('orderStore', () => {
  const orders = ref<OrderItem[]>([]);
  const loading = ref(false);

  const loadOrders = async () => {
    loading.value = true;
    try {
      const { data } = await listOrders();
      orders.value = data;
    } finally {
      loading.value = false;
    }
  };

  const submitOrder = async (payload: OrderCreatePayload) => {
    const { data } = await createOrder(payload);
    orders.value = [data, ...orders.value];
    const goodsStore = useGoodsStore();
    goodsStore.loadGoods();
    goodsStore.loadMyGoods();
    return data;
  };

  const changeOrderStatus = async (orderId: number, payload: OrderUpdateStatusPayload) => {
    const { data } = await updateOrderStatus(orderId, payload);
    orders.value = orders.value.map((order) => (order.id === orderId ? data : order));
    if (payload.status === 'CANCELED') {
      const goodsStore = useGoodsStore();
      goodsStore.loadGoods();
      goodsStore.loadMyGoods();
    }
    return data;
  };

  return {
    orders,
    loading,
    loadOrders,
    submitOrder,
    changeOrderStatus
  };
});
