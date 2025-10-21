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
  quantity: number;
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
  quantity: number;
  coverImageUrl?: string;
}

export interface GoodsUpdatePayload {
  description: string;
  price: number;
  quantity: number;
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

export interface HotGoodsItem {
  goods: GoodsItem;
  score: number;
}

export const fetchHotGoods = (limit = 6) =>
  request.get<HotGoodsItem[]>('/goods/hot', { params: { limit } });

export const recordGoodsView = (id: number) =>
  request.post(`/goods/${id}/view`);

export type RankingMetric = 'orders' | 'carts' | 'views';

export const fetchGoodsRanking = (metric: RankingMetric, limit = 10) =>
  request.get<HotGoodsItem[]>('/goods/ranking', { params: { metric, limit } });
