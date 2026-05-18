import { deleteCookie, getCookie } from '../lib/cookies';

export const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export class ApiError extends Error {
  status: number;
  body: unknown;

  constructor(message: string, status: number, body: unknown) {
    super(message);
    this.status = status;
    this.body = body;
  }
}

export interface ApiFetchOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  skipAuth?: boolean;
  headers?: Record<string, string>;
}

export async function apiFetch<T = unknown>(
  path: string,
  { method = 'GET', body, skipAuth = false, headers = {} }: ApiFetchOptions = {},
): Promise<T> {
  const token = getCookie('access_token');
  const hadAuth = !!token && !skipAuth;
  const finalHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(hadAuth ? { Authorization: `Bearer ${token}` } : {}),
    ...headers,
  };

  let res: Response;
  try {
    res = await fetch(`${BASE_URL}${path}`, {
      method,
      headers: finalHeaders,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError('Falha de rede ao conectar ao servidor', 0, null);
  }

  if (res.status === 401 && !skipAuth) {
    if (hadAuth) {
      deleteCookie('access_token');
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
      throw new ApiError('Sessão expirada', 401, null);
    }
    const errorBody = await res.json().catch(() => ({}));
    const message = errorBody.detail || errorBody.message || 'Não autenticado';
    throw new ApiError(message, 401, errorBody);
  }

  if (!res.ok) {
    const errorBody = await res.json().catch(() => ({}));
    const message = errorBody.detail || errorBody.message || `HTTP ${res.status}`;
    throw new ApiError(message, res.status, errorBody);
  }

  if (res.status === 204) return null as T;
  const text = await res.text();
  if (!text) return null as T;
  return JSON.parse(text) as T;
}
