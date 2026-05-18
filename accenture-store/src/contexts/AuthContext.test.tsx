import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { AuthProvider, useAuth } from './AuthContext';

vi.mock('../api/customers', () => ({ getCustomer: vi.fn() }));
vi.mock('jwt-decode', () => ({ jwtDecode: vi.fn() }));

import { getCustomer } from '../api/customers';
import { jwtDecode } from 'jwt-decode';
import { ApiError } from '../api/client';

const wrapper = ({ children }: { children: ReactNode }) => (
  <AuthProvider>{children}</AuthProvider>
);

const getCustomerMock = getCustomer as unknown as ReturnType<typeof vi.fn>;
const jwtDecodeMock = jwtDecode as unknown as ReturnType<typeof vi.fn>;

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear();
    document.cookie.split(';').forEach((c) => {
      const name = c.split('=')[0].trim();
      if (name) document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
    });
    getCustomerMock.mockReset();
    getCustomerMock.mockResolvedValue({});
    jwtDecodeMock.mockReset();
    jwtDecodeMock.mockReturnValue({});
  });

  it('useAuth fora do provider lança erro', () => {
    expect(() => renderHook(() => useAuth())).toThrow(/useAuth must be used within AuthProvider/);
  });

  it('estado inicial é não autenticado', () => {
    const { result } = renderHook(() => useAuth(), { wrapper });
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.isAdmin).toBe(false);
    expect(result.current.token).toBeNull();
    expect(result.current.user).toBeNull();
  });

  it('login decodifica JWT e popula o user', () => {
    jwtDecodeMock.mockReturnValue({
      customerId: 'c1', email: 'a@b.c', roles: ['USER'], name: 'João',
    });
    const { result } = renderHook(() => useAuth(), { wrapper });

    act(() => result.current.login('jwt-token'));

    expect(result.current.token).toBe('jwt-token');
    expect(result.current.user).toEqual({
      customerId: 'c1', email: 'a@b.c', roles: ['USER'], name: 'João',
    });
    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.isAdmin).toBe(false);
  });

  it('login com role ADMIN deixa isAdmin=true', () => {
    jwtDecodeMock.mockReturnValue({ customerId: 'c1', roles: ['ADMIN'] });
    const { result } = renderHook(() => useAuth(), { wrapper });
    act(() => result.current.login('t'));
    expect(result.current.isAdmin).toBe(true);
  });

  it('login com extra sobrescreve dados do JWT', () => {
    jwtDecodeMock.mockReturnValue({ customerId: 'jwt-c', email: 'jwt@x.com', roles: ['USER'] });
    const { result } = renderHook(() => useAuth(), { wrapper });
    act(() => result.current.login('t', { customerId: 'ovr', email: 'ovr@x.com', name: 'Override', roles: ['ADMIN'] }));
    expect(result.current.user).toMatchObject({ customerId: 'ovr', email: 'ovr@x.com', name: 'Override', roles: ['ADMIN'] });
  });

  it('login com JWT inválido cai em claims vazios sem quebrar', () => {
    jwtDecodeMock.mockImplementation(() => { throw new Error('bad'); });
    const { result } = renderHook(() => useAuth(), { wrapper });
    act(() => result.current.login('t', { customerId: 'c1' }));
    expect(result.current.user?.customerId).toBe('c1');
    expect(result.current.user?.roles).toEqual([]);
  });

  it('login usa sub quando customerId não vem nos claims', () => {
    jwtDecodeMock.mockReturnValue({ sub: 'sub-id', roles: [] });
    const { result } = renderHook(() => useAuth(), { wrapper });
    act(() => result.current.login('t'));
    expect(result.current.user?.customerId).toBe('sub-id');
  });

  it('persiste token em cookie e user em localStorage após login', () => {
    jwtDecodeMock.mockReturnValue({ customerId: 'c1', roles: [] });
    const { result } = renderHook(() => useAuth(), { wrapper });
    act(() => result.current.login('jwt-1'));
    expect(document.cookie).toContain('access_token=jwt-1');
    expect(localStorage.getItem('user')).toBeTruthy();
  });

  it('logout limpa cookie e localStorage', () => {
    jwtDecodeMock.mockReturnValue({ customerId: 'c1', roles: [] });
    const { result } = renderHook(() => useAuth(), { wrapper });
    act(() => result.current.login('t'));
    act(() => result.current.logout());
    expect(result.current.isAuthenticated).toBe(false);
    expect(document.cookie).not.toContain('access_token=t');
    expect(localStorage.getItem('user')).toBeNull();
  });

  it('carrega estado inicial a partir do cookie + localStorage', () => {
    document.cookie = 'access_token=existing-token; path=/';
    localStorage.setItem('user', JSON.stringify({ customerId: 'c1', email: 'a@b.c', roles: ['USER'], name: 'X' }));
    const { result } = renderHook(() => useAuth(), { wrapper });
    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.user?.name).toBe('X');
  });

  it('inicial com user JSON inválido deixa user null mas mantém token', () => {
    document.cookie = 'access_token=existing-token; path=/';
    localStorage.setItem('user', '{invalid');
    const { result } = renderHook(() => useAuth(), { wrapper });
    expect(result.current.token).toBe('existing-token');
    expect(result.current.user).toBeNull();
  });

  it('busca customer e atualiza o nome quando há customerId', async () => {
    document.cookie = 'access_token=t; path=/';
    localStorage.setItem('user', JSON.stringify({ customerId: 'c1', roles: [], name: '' }));
    getCustomerMock.mockResolvedValue({ name: 'Nome Atualizado' });

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => expect(result.current.user?.name).toBe('Nome Atualizado'));
    expect(getCustomerMock).toHaveBeenCalledWith('c1');
  });

  it('não atualiza nome quando customer.name é vazio', async () => {
    document.cookie = 'access_token=t; path=/';
    localStorage.setItem('user', JSON.stringify({ customerId: 'c1', roles: [], name: 'Antigo' }));
    getCustomerMock.mockResolvedValue({ name: '   ' });

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => expect(getCustomerMock).toHaveBeenCalled());
    expect(result.current.user?.name).toBe('Antigo');
  });

  it('quando getCustomer responde 401 limpa a sessão', async () => {
    document.cookie = 'access_token=t; path=/';
    localStorage.setItem('user', JSON.stringify({ customerId: 'c1', roles: [], name: 'X' }));
    getCustomerMock.mockRejectedValue(new ApiError('expirado', 401, null));

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => expect(result.current.isAuthenticated).toBe(false));
  });

  it('outros erros do getCustomer não derrubam sessão', async () => {
    document.cookie = 'access_token=t; path=/';
    localStorage.setItem('user', JSON.stringify({ customerId: 'c1', roles: [], name: 'X' }));
    getCustomerMock.mockRejectedValue(new ApiError('boom', 500, null));

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => expect(getCustomerMock).toHaveBeenCalled());
    expect(result.current.isAuthenticated).toBe(true);
  });
});
