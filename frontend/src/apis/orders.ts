import request from './request';

export interface OrderItem {
  id: number;
  goodsId: number;
  sellerId: number;
  buyerId: number;
  status: string;
  createdAt: string;
  updatedAt: string;
  goodsTitle: string;
  goodsCoverImageUrl?: string;
  sellerNickname: string;
  buyerNickname: string;
}

export interface OrderCreatePayload {
  goodsId: number;
}

export interface OrderUpdateStatusPayload {
  status: string;
}

export const createOrder = (payload: OrderCreatePayload) =>
  request.post<OrderItem>('/orders', payload);

export const listOrders = () => request.get<OrderItem[]>('/orders');

export const updateOrderStatus = (orderId: number, payload: OrderUpdateStatusPayload) =>
  request.patch<OrderItem>(`/orders/${orderId}`, payload);
