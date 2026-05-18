import { describe, it, expect, beforeEach, vi } from 'vitest';

vi.mock('./client', () => ({ apiFetch: vi.fn() }));

import { apiFetch } from './client';
import { login, register, forgotPassword, resetPassword } from './auth';

const apiMock = apiFetch as unknown as ReturnType<typeof vi.fn>;

describe('api/auth', () => {
  beforeEach(() => apiMock.mockReset());

  it('login chama o endpoint correto sem auth', async () => {
    apiMock.mockResolvedValue({ token: 'x' });
    await login('a@b.c', 'pwd');
    expect(apiMock).toHaveBeenCalledWith('/api/v1/auth/login', {
      method: 'POST',
      body: { email: 'a@b.c', password: 'pwd' },
      skipAuth: true,
    });
  });

  it('register passa o payload completo sem auth', async () => {
    apiMock.mockResolvedValue(null);
    const payload = { email: 'a@b.c', password: 'pwd', name: 'João', cpf: '1', phone: '2' };
    await register(payload);
    expect(apiMock).toHaveBeenCalledWith('/api/v1/gateway/register-flow', {
      method: 'POST',
      body: payload,
      skipAuth: true,
    });
  });

  it('forgotPassword envia apenas o email sem auth', async () => {
    apiMock.mockResolvedValue(undefined);
    await forgotPassword('a@b.c');
    expect(apiMock).toHaveBeenCalledWith('/api/v1/auth/forgot-password', {
      method: 'POST',
      body: { email: 'a@b.c' },
      skipAuth: true,
    });
  });

  it('resetPassword envia email/token/senha sem auth', async () => {
    apiMock.mockResolvedValue(undefined);
    await resetPassword('a@b.c', 'tk', 'newpwd');
    expect(apiMock).toHaveBeenCalledWith('/api/v1/auth/reset-password', {
      method: 'POST',
      body: { email: 'a@b.c', token: 'tk', newPassword: 'newpwd' },
      skipAuth: true,
    });
  });
});
