import { describe, it, expect, beforeEach, vi } from 'vitest';

vi.mock('./client', () => ({ apiFetch: vi.fn() }));

import { apiFetch } from './client';
import {
  getCustomer, updateCustomer, listCustomers, deleteCustomer,
  listAddresses, getAddress, createAddress, updateAddress, deleteAddress,
} from './customers';

const apiMock = apiFetch as unknown as ReturnType<typeof vi.fn>;
const sampleAddress = {
  zipCode: '01000-000', street: 'Rua A', number: '1', city: 'SP', state: 'SP',
};

describe('api/customers', () => {
  beforeEach(() => apiMock.mockReset());

  it('getCustomer GET /customers/:id', async () => {
    apiMock.mockResolvedValue({});
    await getCustomer('c1');
    expect(apiMock).toHaveBeenCalledWith('/customers/c1');
  });

  it('updateCustomer envia PATCH', async () => {
    apiMock.mockResolvedValue({});
    await updateCustomer('c1', { name: 'Novo' });
    expect(apiMock).toHaveBeenCalledWith('/customers/c1', { method: 'PATCH', body: { name: 'Novo' } });
  });

  it('listCustomers usa defaults e custom', async () => {
    apiMock.mockResolvedValue([]);
    await listCustomers();
    expect(apiMock).toHaveBeenCalledWith('/customers?page=0&size=20');
    await listCustomers(3, 50);
    expect(apiMock).toHaveBeenCalledWith('/customers?page=3&size=50');
  });

  it('deleteCustomer envia DELETE', async () => {
    apiMock.mockResolvedValue(undefined);
    await deleteCustomer('c1');
    expect(apiMock).toHaveBeenCalledWith('/customers/c1', { method: 'DELETE' });
  });

  it('listAddresses', async () => {
    apiMock.mockResolvedValue([]);
    await listAddresses('c1');
    expect(apiMock).toHaveBeenCalledWith('/customers/c1/addresses');
  });

  it('getAddress', async () => {
    apiMock.mockResolvedValue({});
    await getAddress('c1', 'a1');
    expect(apiMock).toHaveBeenCalledWith('/customers/c1/addresses/a1');
  });

  it('createAddress envia POST com endereço', async () => {
    apiMock.mockResolvedValue({});
    await createAddress('c1', sampleAddress);
    expect(apiMock).toHaveBeenCalledWith('/customers/c1/addresses', { method: 'POST', body: sampleAddress });
  });

  it('updateAddress envia PUT', async () => {
    apiMock.mockResolvedValue({});
    await updateAddress('c1', 'a1', sampleAddress);
    expect(apiMock).toHaveBeenCalledWith('/customers/c1/addresses/a1', { method: 'PUT', body: sampleAddress });
  });

  it('deleteAddress envia DELETE', async () => {
    apiMock.mockResolvedValue(undefined);
    await deleteAddress('c1', 'a1');
    expect(apiMock).toHaveBeenCalledWith('/customers/c1/addresses/a1', { method: 'DELETE' });
  });
});
