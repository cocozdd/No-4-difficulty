import request from './request';

export interface GoodsFilterParams {
  category?: string;
  minPrice?: number;
  maxPrice?: number;
  keyword?: string;
}

export type GoodsStatus = 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED';

export interface GoodsItem {
  id: number;
  title: string;
  description: string;
  category: string;
  price: number;
  coverImageUrl?: string;
  publishedAt: string;
  sellerId: number;
  sellerNickname: string;
  sold: boolean;
  status: GoodsStatus;
}

export interface GoodsCreatePayload {
  title: string;
  description: string;
  category: string;
  price: number;
  coverImageUrl?: string;
}

export interface GoodsUpdatePayload {
  description: string;
  price: number;
  coverImageUrl?: string;
}

export const fetchGoods = (params?: GoodsFilterParams) =>
  request.get<GoodsItem[]>('/goods', { params });

export const fetchGoodsDetail = (id: number) =>
  request.get<GoodsItem>(`/goods/${id}`);

export const createGoods = (payload: GoodsCreatePayload) =>
  request.post<GoodsItem>('/goods', payload);

export const fetchMyGoods = () => request.get<GoodsItem[]>('/goods/mine');

export const updateGoods = (id: number, payload: GoodsUpdatePayload) =>
  request.put<GoodsItem>(`/goods/${id}`, payload);

export const deleteGoods = (id: number) => request.delete(`/goods/${id}`);

export interface GoodsReviewPayload {
  status: GoodsStatus;
}

export const fetchPendingGoods = () => request.get<GoodsItem[]>('/goods/pending');

export const reviewGoods = (id: number, payload: GoodsReviewPayload) =>
  request.put<GoodsItem>(`/goods/${id}/review`, payload);
