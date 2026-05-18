import { apiFetch } from './client';
import type {
  Paginated, TopUpRequest, TopUpResponse, TopUpSubmitResponse,
  Wallet, WalletOwnerType, WalletTransaction,
} from './types';

export const COMPANY_OWNER_ID = '00000000-0000-0000-0000-000000000001';

export const getWalletByOwner = (ownerId: string, type: WalletOwnerType = 'CUSTOMER') =>
  apiFetch<Wallet>(`/wallets/owners/${type}/${ownerId}`);

export const listWalletTransactions = (walletId: string, page = 0, size = 20) =>
  apiFetch<Paginated<WalletTransaction> | WalletTransaction[]>(
    `/wallets/${walletId}/transactions?page=${page}&size=${size}`,
  );

export const listTransactionsByOwner = (
  ownerId: string,
  type: WalletOwnerType = 'CUSTOMER',
) =>
  apiFetch<Paginated<WalletTransaction> | WalletTransaction[]>(
    `/wallets/owners/${type}/${ownerId}/transactions`,
  );

export const listCompanyTransactions = (ownerId = COMPANY_OWNER_ID) =>
  listTransactionsByOwner(ownerId, 'COMPANY');

export const createTopUp = (walletId: string, payload: TopUpRequest) =>
  apiFetch<TopUpResponse>(`/wallets/${walletId}/top-ups`, { method: 'POST', body: payload });

export const submitTopUp = (topUpId: string) =>
  apiFetch<TopUpSubmitResponse>(`/wallets/top-ups/${topUpId}/submit`, { method: 'POST' });
