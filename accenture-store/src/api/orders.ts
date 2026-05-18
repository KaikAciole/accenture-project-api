import { apiFetch } from './client';
import type { CreateOrderRequest, Order, Paginated } from './types';

export const createOrder = (order: CreateOrderRequest) =>
  apiFetch<Order>('/orders', { method: 'POST', body: order });

export const getOrder = (id: string) => apiFetch<Order>(`/orders/${id}`);

export const listMyOrders = (page = 0, size = 10) =>
  apiFetch<Paginated<Order> | Order[]>(`/orders/my-orders?page=${page}&size=${size}`);

export const listAllOrders = (page = 0, size = 10) =>
  apiFetch<Paginated<Order> | Order[]>(`/orders?page=${page}&size=${size}`);

export const cancelOrder = (id: string) =>
  apiFetch<Order>(`/orders/${id}/cancel`, { method: 'PATCH' });
