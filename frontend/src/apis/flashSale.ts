import request from './request';

export interface FlashSaleItemCreatePayload {
  title: string;
  description?: string;
  originalPrice: number;
  flashPrice: number;
  totalStock: number;
  startTime: string;
  endTime: string;
}

export interface FlashSaleItem {
  id: number;
  title: string;
  description?: string;
  originalPrice: number;
  flashPrice: number;
  totalStock: number;
  remainingStock: number;
  startTime: string;
  endTime: string;
  status: 'SCHEDULED' | 'RUNNING' | 'ENDED';
}

export const createFlashSaleItem = (payload: FlashSaleItemCreatePayload) =>
  request.post('/flash-sale/items', payload);

export const fetchFlashSaleItems = () =>
  request.get<FlashSaleItem[]>('/flash-sale/items');

export const purchaseFlashSaleItem = (flashSaleItemId: number) =>
  request.post('/flash-sale/purchase', { flashSaleItemId });
