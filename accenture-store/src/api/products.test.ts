import { describe, it, expect, beforeEach, vi } from 'vitest';

vi.mock('./client', () => ({ apiFetch: vi.fn() }));

import { apiFetch } from './client';
import {
  listProducts, getProduct, getProductBySku, createProduct, updateProduct, deleteProduct,
} from './products';

const apiMock = apiFetch as unknown as ReturnType<typeof vi.fn>;

describe('api/products', () => {
  beforeEach(() => apiMock.mockReset());

  it('listProducts sem args usa defaults page=0 size=20', async () => {
    apiMock.mockResolvedValue([]);
    await listProducts();
    expect(apiMock).toHaveBeenCalledWith('/products?page=0&size=20');
  });

  it('listProducts com page/size customizados', async () => {
    apiMock.mockResolvedValue([]);
    await listProducts({ page: 2, size: 5 });
    expect(apiMock).toHaveBeenCalledWith('/products?page=2&size=5');
  });

  it('listProducts com query usa /products/search', async () => {
    apiMock.mockResolvedValue([]);
    await listProducts({ q: 'caneca' });
    expect(apiMock).toHaveBeenCalledWith('/products/search?name=caneca');
  });

  it('getProduct usa id na URL', async () => {
    apiMock.mockResolvedValue({});
    await getProduct('abc');
    expect(apiMock).toHaveBeenCalledWith('/products/abc');
  });

  it('getProductBySku usa o caminho /sku/', async () => {
    apiMock.mockResolvedValue({});
    await getProductBySku('SKU-1');
    expect(apiMock).toHaveBeenCalledWith('/products/sku/SKU-1');
  });

  it('createProduct envia POST com payload', async () => {
    apiMock.mockResolvedValue({});
    const payload = { sku: 'S', name: 'N', category: 'C', basePrice: 1, stockQuantity: 1 };
    await createProduct(payload);
    expect(apiMock).toHaveBeenCalledWith('/products', { method: 'POST', body: payload });
  });

  it('updateProduct envia PUT com id e payload', async () => {
    apiMock.mockResolvedValue({});
    const payload = { sku: 'S', name: 'N', category: 'C', basePrice: 2, stockQuantity: 3 };
    await updateProduct('id-1', payload);
    expect(apiMock).toHaveBeenCalledWith('/products/id-1', { method: 'PUT', body: payload });
  });

  it('deleteProduct envia DELETE', async () => {
    apiMock.mockResolvedValue(undefined);
    await deleteProduct('id-1');
    expect(apiMock).toHaveBeenCalledWith('/products/id-1', { method: 'DELETE' });
  });
});
