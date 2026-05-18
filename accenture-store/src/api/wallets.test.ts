import { describe, it, expect, beforeEach, vi } from 'vitest';

vi.mock('./client', () => ({ apiFetch: vi.fn() }));

import { apiFetch } from './client';
import {
  COMPANY_OWNER_ID, getWalletByOwner, listWalletTransactions, listTransactionsByOwner,
  listCompanyTransactions, createTopUp, submitTopUp,
} from './wallets';

const apiMock = apiFetch as unknown as ReturnType<typeof vi.fn>;

describe('api/wallets', () => {
  beforeEach(() => apiMock.mockReset());

  it('COMPANY_OWNER_ID é uma UUID fixa', () => {
    expect(COMPANY_OWNER_ID).toBe('00000000-0000-0000-0000-000000000001');
  });

  it('getWalletByOwner default CUSTOMER', async () => {
    apiMock.mockResolvedValue({});
    await getWalletByOwner('o1');
    expect(apiMock).toHaveBeenCalledWith('/wallets/owners/CUSTOMER/o1');
  });

  it('getWalletByOwner respeita tipo', async () => {
    apiMock.mockResolvedValue({});
    await getWalletByOwner('o1', 'COMPANY');
    expect(apiMock).toHaveBeenCalledWith('/wallets/owners/COMPANY/o1');
  });

  it('listWalletTransactions com defaults e custom', async () => {
    apiMock.mockResolvedValue([]);
    await listWalletTransactions('w1');
    expect(apiMock).toHaveBeenCalledWith('/wallets/w1/transactions?page=0&size=20');
    await listWalletTransactions('w1', 2, 5);
    expect(apiMock).toHaveBeenCalledWith('/wallets/w1/transactions?page=2&size=5');
  });

  it('listTransactionsByOwner monta path correto', async () => {
    apiMock.mockResolvedValue([]);
    await listTransactionsByOwner('o1');
    expect(apiMock).toHaveBeenCalledWith('/wallets/owners/CUSTOMER/o1/transactions');
    await listTransactionsByOwner('o2', 'COMPANY');
    expect(apiMock).toHaveBeenCalledWith('/wallets/owners/COMPANY/o2/transactions');
  });

  it('listCompanyTransactions usa COMPANY e default owner', async () => {
    apiMock.mockResolvedValue([]);
    await listCompanyTransactions();
    expect(apiMock).toHaveBeenCalledWith(`/wallets/owners/COMPANY/${COMPANY_OWNER_ID}/transactions`);
    await listCompanyTransactions('outro');
    expect(apiMock).toHaveBeenCalledWith('/wallets/owners/COMPANY/outro/transactions');
  });

  it('createTopUp POST', async () => {
    apiMock.mockResolvedValue({});
    const payload = { customerId: 'c1', amount: 100, customerEmail: 'a@b.c' };
    await createTopUp('w1', payload);
    expect(apiMock).toHaveBeenCalledWith('/wallets/w1/top-ups', { method: 'POST', body: payload });
  });

  it('submitTopUp POST', async () => {
    apiMock.mockResolvedValue({});
    await submitTopUp('t1');
    expect(apiMock).toHaveBeenCalledWith('/wallets/top-ups/t1/submit', { method: 'POST' });
  });
});
