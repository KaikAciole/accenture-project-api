import { describe, it, expect, beforeEach, vi, type Mock } from 'vitest';
import { apiFetch, ApiError, BASE_URL } from './client';
import { setCookie } from '../lib/cookies';

function mockResponse(body: unknown, init: Partial<{ status: number; ok: boolean }> = {}) {
  const status = init.status ?? 200;
  const ok = init.ok ?? (status >= 200 && status < 300);
  return {
    ok,
    status,
    json: vi.fn().mockResolvedValue(body),
    text: vi.fn().mockResolvedValue(body == null ? '' : JSON.stringify(body)),
  } as unknown as Response;
}

describe('api/client', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn());
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { pathname: '/', href: '/' },
    });
  });

  it('faz GET autenticado adicionando o Bearer token e prefixo da BASE_URL', async () => {
    setCookie('access_token', 'jwt-xyz');
    (fetch as unknown as Mock).mockResolvedValueOnce(mockResponse({ ok: true }));

    await apiFetch('/products');

    expect(fetch).toHaveBeenCalledTimes(1);
    const [url, init] = (fetch as unknown as Mock).mock.calls[0];
    expect(url).toBe(`${BASE_URL}/products`);
    expect(init.method).toBe('GET');
    expect(init.headers.Authorization).toBe('Bearer jwt-xyz');
    expect(init.headers['Content-Type']).toBe('application/json');
  });

  it('omite Authorization quando skipAuth é true', async () => {
    setCookie('access_token', 'jwt-xyz');
    (fetch as unknown as Mock).mockResolvedValueOnce(mockResponse({ ok: true }));

    await apiFetch('/auth/login', { method: 'POST', body: { email: 'a@b.c' }, skipAuth: true });

    const [, init] = (fetch as unknown as Mock).mock.calls[0];
    expect(init.headers.Authorization).toBeUndefined();
    expect(init.body).toBe(JSON.stringify({ email: 'a@b.c' }));
  });

  it('parseia JSON quando o status é 2xx', async () => {
    (fetch as unknown as Mock).mockResolvedValueOnce(mockResponse({ id: '1', name: 'A' }));
    const data = await apiFetch<{ id: string; name: string }>('/products/1');
    expect(data).toEqual({ id: '1', name: 'A' });
  });

  it('retorna null em 204 sem chamar text', async () => {
    (fetch as unknown as Mock).mockResolvedValueOnce(mockResponse(null, { status: 204 }));
    const data = await apiFetch('/products/1', { method: 'DELETE' });
    expect(data).toBeNull();
  });

  it('lança ApiError com mensagem do body quando a resposta não é ok', async () => {
    (fetch as unknown as Mock).mockResolvedValueOnce(
      mockResponse({ detail: 'Produto não encontrado' }, { status: 404, ok: false }),
    );

    await expect(apiFetch('/products/x')).rejects.toMatchObject({
      name: 'Error',
      status: 404,
      message: 'Produto não encontrado',
    });
  });

  it('em 401 sem skipAuth: limpa sessão e redireciona para /login', async () => {
    setCookie('access_token', 'expirado');
    localStorage.setItem('user', JSON.stringify({ name: 'X' }));
    (fetch as unknown as Mock).mockResolvedValueOnce(mockResponse(null, { status: 401, ok: false }));

    await expect(apiFetch('/me')).rejects.toBeInstanceOf(ApiError);

    expect(document.cookie).not.toContain('access_token=expirado');
    expect(localStorage.getItem('user')).toBeNull();
    expect(window.location.href).toBe('/login');
  });

  it('em 401 com skipAuth: propaga como erro normal sem redirecionar', async () => {
    (fetch as unknown as Mock).mockResolvedValueOnce(
      mockResponse({ message: 'credenciais inválidas' }, { status: 401, ok: false }),
    );

    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { pathname: '/login', href: '/login' },
    });

    await expect(
      apiFetch('/auth/login', { method: 'POST', body: { email: 'x' }, skipAuth: true }),
    ).rejects.toMatchObject({ status: 401, message: 'credenciais inválidas' });
    expect(window.location.href).toBe('/login');
  });

  it('encapsula falha de rede em ApiError com status 0', async () => {
    (fetch as unknown as Mock).mockRejectedValueOnce(new TypeError('Failed to fetch'));

    await expect(apiFetch('/x')).rejects.toMatchObject({
      status: 0,
      message: 'Falha de rede ao conectar ao servidor',
    });
  });
});
