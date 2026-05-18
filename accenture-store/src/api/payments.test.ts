import { describe, it, expect, beforeEach, vi } from 'vitest';

vi.mock('./client', () => ({ apiFetch: vi.fn() }));

import { apiFetch } from './client';
import { createPayment, processPayment, getPayment, getPaymentByOrder } from './payments';

const apiMock = apiFetch as unknown as ReturnType<typeof vi.fn>;

describe('api/payments', () => {
  beforeEach(() => apiMock.mockReset());

  it('createPayment POST /payments', async () => {
    apiMock.mockResolvedValue({});
    const req = { orderId: 'o1', customerId: 'c1', amount: 10, method: 'WALLET' as const };
    await createPayment(req);
    expect(apiMock).toHaveBeenCalledWith('/payments', { method: 'POST', body: req });
  });

  it('processPayment PATCH /payments/:id/process', async () => {
    apiMock.mockResolvedValue({});
    await processPayment('p1');
    expect(apiMock).toHaveBeenCalledWith('/payments/p1/process', { method: 'PATCH' });
  });

  it('getPayment GET /payments/:id', async () => {
    apiMock.mockResolvedValue({});
    await getPayment('p1');
    expect(apiMock).toHaveBeenCalledWith('/payments/p1');
  });

  it('getPaymentByOrder GET /payments/orders/:id', async () => {
    apiMock.mockResolvedValue({});
    await getPaymentByOrder('o1');
    expect(apiMock).toHaveBeenCalledWith('/payments/orders/o1');
  });
});
