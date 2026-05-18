import { describe, it, expect, beforeEach, vi } from 'vitest';

vi.mock('./client', () => ({ apiFetch: vi.fn() }));

import { apiFetch } from './client';
import { lookupCep } from './cep';

const apiMock = apiFetch as unknown as ReturnType<typeof vi.fn>;

describe('api/cep', () => {
  beforeEach(() => apiMock.mockReset());

  it('limpa caracteres não numéricos antes de enviar', async () => {
    apiMock.mockResolvedValue({});
    await lookupCep('01.000-000');
    expect(apiMock).toHaveBeenCalledWith('/api/v1/cep/lookup', {
      method: 'POST',
      body: { cep: '01000000' },
    });
  });

  it('aceita já limpo', async () => {
    apiMock.mockResolvedValue({});
    await lookupCep('99887766');
    expect(apiMock).toHaveBeenCalledWith('/api/v1/cep/lookup', {
      method: 'POST',
      body: { cep: '99887766' },
    });
  });
});
