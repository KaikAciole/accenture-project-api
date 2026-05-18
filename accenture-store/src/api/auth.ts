import { apiFetch } from './client';
import type { AuthLoginResponse, RegisterPayload } from './types';

export const login = (email: string, password: string) =>
  apiFetch<AuthLoginResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: { email, password },
    skipAuth: true,
  });

export const register = (payload: RegisterPayload) =>
  apiFetch<unknown>('/api/v1/gateway/register-flow', {
    method: 'POST',
    body: payload,
    skipAuth: true,
  });

export const forgotPassword = (email: string) =>
  apiFetch<void>('/api/v1/auth/forgot-password', {
    method: 'POST',
    body: { email },
    skipAuth: true,
  });

export const resetPassword = (email: string, token: string, newPassword: string) =>
  apiFetch<void>('/api/v1/auth/reset-password', {
    method: 'POST',
    body: { email, token, newPassword },
    skipAuth: true,
  });
