import { describe, it, expect, beforeEach, vi } from 'vitest';

vi.mock('./client', () => ({ apiFetch: vi.fn() }));

import { apiFetch } from './client';
import { createOrder, getOrder, listMyOrders, listAllOrders, cancelOrder } from './orders';

const apiMock = apiFetch as unknown as ReturnType<typeof vi.fn>;

describe('api/orders', () => {
  beforeEach(() => apiMock.mockReset());

  it('createOrder POST /orders', async () => {
    apiMock.mockResolvedValue({});
    const req = { addressId: 'a1', items: [{ sku: 'S', quantity: 1, unitPrice: 10 }] };
    await createOrder(req);
    expect(apiMock).toHaveBeenCalledWith('/orders', { method: 'POST', body: req });
  });

  it('getOrder GET /orders/:id', async () => {
    apiMock.mockResolvedValue({});
    await getOrder('o1');
    expect(apiMock).toHaveBeenCalledWith('/orders/o1');
  });

  it('listMyOrders default e custom', async () => {
    apiMock.mockResolvedValue([]);
    await listMyOrders();
    expect(apiMock).toHaveBeenCalledWith('/orders/my-orders?page=0&size=10');
    await listMyOrders(2, 5);
    expect(apiMock).toHaveBeenCalledWith('/orders/my-orders?page=2&size=5');
  });

  it('listAllOrders default e custom', async () => {
    apiMock.mockResolvedValue([]);
    await listAllOrders();
    expect(apiMock).toHaveBeenCalledWith('/orders?page=0&size=10');
    await listAllOrders(1, 20);
    expect(apiMock).toHaveBeenCalledWith('/orders?page=1&size=20');
  });

  it('cancelOrder PATCH /orders/:id/cancel', async () => {
    apiMock.mockResolvedValue({});
    await cancelOrder('o1');
    expect(apiMock).toHaveBeenCalledWith('/orders/o1/cancel', { method: 'PATCH' });
  });
});
