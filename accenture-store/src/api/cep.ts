import { apiFetch } from './client';
import type { CepLookup } from './types';

export const lookupCep = (cep: string) =>
  apiFetch<CepLookup>('/api/v1/cep/lookup', {
    method: 'POST',
    body: { cep: cep.replace(/\D/g, '') },
  });
