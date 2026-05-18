import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import type { ReactNode } from 'react';
import { ThemeModeProvider, useThemeMode } from './ThemeModeContext';

const wrapper = ({ children }: { children: ReactNode }) => (
  <ThemeModeProvider>{children}</ThemeModeProvider>
);

describe('ThemeModeContext', () => {
  beforeEach(() => {
    localStorage.clear();
    window.matchMedia = (query: string) => ({
      matches: false, media: query, onchange: null,
      addListener: () => {}, removeListener: () => {},
      addEventListener: () => {}, removeEventListener: () => {}, dispatchEvent: () => false,
    });
  });

  it('default light quando não há nada salvo e prefers-color-scheme não é dark', () => {
    const { result } = renderHook(() => useThemeMode(), { wrapper });
    expect(result.current.mode).toBe('light');
  });

  it('respeita prefers-color-scheme: dark quando não tem storage', () => {
    window.matchMedia = (query: string) => ({
      matches: query === '(prefers-color-scheme: dark)',
      media: query, onchange: null,
      addListener: () => {}, removeListener: () => {},
      addEventListener: () => {}, removeEventListener: () => {}, dispatchEvent: () => false,
    });
    const { result } = renderHook(() => useThemeMode(), { wrapper });
    expect(result.current.mode).toBe('dark');
  });

  it('carrega preferência salva (dark)', () => {
    localStorage.setItem('themeMode', 'dark');
    const { result } = renderHook(() => useThemeMode(), { wrapper });
    expect(result.current.mode).toBe('dark');
  });

  it('ignora valor inválido no storage e cai no default', () => {
    localStorage.setItem('themeMode', 'roxo');
    const { result } = renderHook(() => useThemeMode(), { wrapper });
    expect(result.current.mode).toBe('light');
  });

  it('toggleMode alterna e persiste no localStorage', () => {
    const { result } = renderHook(() => useThemeMode(), { wrapper });
    expect(result.current.mode).toBe('light');
    act(() => result.current.toggleMode());
    expect(result.current.mode).toBe('dark');
    expect(localStorage.getItem('themeMode')).toBe('dark');
    act(() => result.current.toggleMode());
    expect(result.current.mode).toBe('light');
    expect(localStorage.getItem('themeMode')).toBe('light');
  });

  it('useThemeMode fora do provider devolve default sem quebrar', () => {
    const { result } = renderHook(() => useThemeMode());
    expect(result.current.mode).toBe('light');
    expect(typeof result.current.toggleMode).toBe('function');
    expect(() => result.current.toggleMode()).not.toThrow();
  });
});
