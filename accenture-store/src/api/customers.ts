import { apiFetch } from './client';
import type { Address, Customer, Paginated } from './types';

export const getCustomer = (id: string) => apiFetch<Customer>(`/customers/${id}`);

export const updateCustomer = (id: string, patch: Partial<Customer>) =>
  apiFetch<Customer>(`/customers/${id}`, { method: 'PATCH', body: patch });

export const listCustomers = (page = 0, size = 20) =>
  apiFetch<Paginated<Customer> | Customer[]>(`/customers?page=${page}&size=${size}`);

export const deleteCustomer = (id: string) =>
  apiFetch<void>(`/customers/${id}`, { method: 'DELETE' });

export const listAddresses = (customerId: string) =>
  apiFetch<Paginated<Address> | Address[]>(`/customers/${customerId}/addresses`);

export const getAddress = (customerId: string, addressId: string) =>
  apiFetch<Address>(`/customers/${customerId}/addresses/${addressId}`);

export const createAddress = (customerId: string, address: Address) =>
  apiFetch<Address>(`/customers/${customerId}/addresses`, { method: 'POST', body: address });

export const updateAddress = (customerId: string, addressId: string, address: Address) =>
  apiFetch<Address>(`/customers/${customerId}/addresses/${addressId}`, { method: 'PUT', body: address });

export const deleteAddress = (customerId: string, addressId: string) =>
  apiFetch<void>(`/customers/${customerId}/addresses/${addressId}`, { method: 'DELETE' });
