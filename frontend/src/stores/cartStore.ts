import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import type {
  CartItem,
  CartItemPayload,
  CartItemUpdatePayload
} from '../apis/cart';
import {
  addCartItem,
  fetchCartItems,
  purgeSoldCartItems,
  removeCartItem,
  updateCartItem
} from '../apis/cart';

export const useCartStore = defineStore('cartStore', () => {
  const items = ref<CartItem[]>([]);
  const loading = ref(false);

  const totalDistinct = computed(() => items.value.length);

  const totalQuantity = computed(() =>
    items.value.reduce((sum, item) => sum + item.quantity, 0)
  );

  const totalAmount = computed(() =>
    items.value.reduce((sum, item) => {
      if (typeof item.goodsPrice === 'number') {
        return sum + item.goodsPrice * item.quantity;
      }
      return sum;
    }, 0)
  );

  const loadCart = async () => {
    loading.value = true;
    try {
      const { data } = await fetchCartItems();
      items.value = data;
    } finally {
      loading.value = false;
    }
  };

  const addToCart = async (payload: CartItemPayload) => {
    const { data } = await addCartItem(payload);
    const existingIndex = items.value.findIndex((item) => item.id === data.id);
    if (existingIndex >= 0) {
      items.value = [
        ...items.value.slice(0, existingIndex),
        data,
        ...items.value.slice(existingIndex + 1)
      ];
    } else {
      items.value = [data, ...items.value];
    }
    return data;
  };

  const changeQuantity = async (cartItemId: number, payload: CartItemUpdatePayload) => {
    const { data } = await updateCartItem(cartItemId, payload);
    const index = items.value.findIndex((item) => item.id === cartItemId);
    if (index >= 0) {
      items.value = [
        ...items.value.slice(0, index),
        data,
        ...items.value.slice(index + 1)
      ];
    }
    return data;
  };

  const removeItem = async (cartItemId: number) => {
    await removeCartItem(cartItemId);
    items.value = items.value.filter((item) => item.id !== cartItemId);
  };

  const purgeSold = async () => {
    const { data } = await purgeSoldCartItems();
    if (data.removed > 0) {
      items.value = items.value.filter(
        (item) => !item.goodsSold && item.goodsStatus === 'APPROVED'
      );
    }
    return data.removed;
  };

  return {
    items,
    loading,
    totalDistinct,
    totalQuantity,
    totalAmount,
    loadCart,
    addToCart,
    changeQuantity,
    removeItem,
    purgeSold
  };
});

