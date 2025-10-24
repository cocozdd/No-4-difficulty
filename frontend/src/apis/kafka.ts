import request from './request';

export interface KafkaDiagnosticsPayload {
  key?: string;
  orderId?: number;
  goodsId?: number;
  buyerId?: number;
  currentStatus?: string;
  previousStatus?: string;
  message?: string;
}

export interface KafkaDiagnosticsResponse {
  topic: string;
  key: string;
  dispatchedAt: string;
  message: string;
}

export const triggerKafkaDiagnostics = async (payload: KafkaDiagnosticsPayload) => {
  const { data } = await request.post<KafkaDiagnosticsResponse>(
    '/diagnostics/kafka/order-events',
    payload
  );
  return data;
};
