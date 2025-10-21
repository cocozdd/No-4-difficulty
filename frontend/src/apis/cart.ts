import request from './request';

export interface CartItem {
  id: number;
  goodsId: number;
  goodsTitle: string;
  goodsCoverImageUrl?: string;
  goodsCategory?: string;
  goodsPrice?: number;
  goodsQuantityAvailable?: number;
  goodsSold: boolean;
  goodsStatus: string;
  quantity: number;
}

export interface CartItemPayload {
  goodsId: number;
  quantity: number;
}

export interface CartItemUpdatePayload {
  quantity: number;
}

export const fetchCartItems = () => request.get<CartItem[]>('/cart');

export const addCartItem = (payload: CartItemPayload) =>
  request.post<CartItem>('/cart', payload);

export const updateCartItem = (cartItemId: number, payload: CartItemUpdatePayload) =>
  request.put<CartItem>(`/cart/${cartItemId}`, payload);

export const removeCartItem = (cartItemId: number) =>
  request.delete(`/cart/${cartItemId}`);

export const purgeSoldCartItems = () =>
  request.delete<{ removed: number }>('/cart/sold');
