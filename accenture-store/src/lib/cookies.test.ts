import { describe, it, expect, beforeEach } from 'vitest';
import { getCookie, setCookie, deleteCookie } from './cookies';

describe('lib/cookies', () => {
  beforeEach(() => {
    document.cookie.split(';').forEach((c) => {
      const name = c.split('=')[0].trim();
      if (name) document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
    });
  });

  describe('setCookie', () => {
    it('grava o valor codificado no document.cookie', () => {
      setCookie('token', 'abc 123');
      expect(document.cookie).toContain('token=abc%20123');
    });

    it('aceita override de dias sem quebrar', () => {
      expect(() => setCookie('foo', 'bar', 30)).not.toThrow();
      expect(document.cookie).toContain('foo=bar');
    });
  });

  describe('getCookie', () => {
    it('retorna null quando o cookie não existe', () => {
      expect(getCookie('inexistente')).toBeNull();
    });

    it('retorna o valor decodificado quando o cookie existe', () => {
      setCookie('session', 'hello world');
      expect(getCookie('session')).toBe('hello world');
    });

    it('isola cookies com nomes parecidos', () => {
      setCookie('access_token', 'A');
      setCookie('refresh_token', 'B');
      expect(getCookie('access_token')).toBe('A');
      expect(getCookie('refresh_token')).toBe('B');
    });
  });

  describe('deleteCookie', () => {
    it('remove o cookie de fato', () => {
      setCookie('temp', 'val');
      expect(getCookie('temp')).toBe('val');
      deleteCookie('temp');
      expect(getCookie('temp')).toBeNull();
    });
  });
});
