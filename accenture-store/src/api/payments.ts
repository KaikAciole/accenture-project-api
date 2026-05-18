import { apiFetch } from './client';
import type { Payment, PaymentRequest } from './types';

export const createPayment = (payment: PaymentRequest) =>
  apiFetch<Payment>('/payments', { method: 'POST', body: payment });

export const processPayment = (id: string) =>
  apiFetch<Payment>(`/payments/${id}/process`, { method: 'PATCH' });

export const getPayment = (id: string) => apiFetch<Payment>(`/payments/${id}`);

export const getPaymentByOrder = (orderId: string) =>
  apiFetch<Payment>(`/payments/orders/${orderId}`);
