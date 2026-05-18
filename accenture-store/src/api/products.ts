import { apiFetch } from './client';
import type { Paginated, Product, ProductPayload } from './types';

export interface ListProductsParams {
  page?: number;
  size?: number;
  q?: string;
}

export const listProducts = ({ page = 0, size = 20, q = '' }: ListProductsParams = {}) => {
  if (q) {
    const params = new URLSearchParams({ name: q });
    return apiFetch<Paginated<Product> | Product[]>(`/products/search?${params.toString()}`);
  }
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  return apiFetch<Paginated<Product> | Product[]>(`/products?${params.toString()}`);
};

export const getProduct = (id: string) => apiFetch<Product>(`/products/${id}`);

export const getProductBySku = (sku: string) => apiFetch<Product>(`/products/sku/${sku}`);

export const createProduct = (payload: ProductPayload) =>
  apiFetch<Product>('/products', { method: 'POST', body: payload });

export const updateProduct = (id: string, payload: ProductPayload) =>
  apiFetch<Product>(`/products/${id}`, { method: 'PUT', body: payload });

export const deleteProduct = (id: string) =>
  apiFetch<void>(`/products/${id}`, { method: 'DELETE' });
