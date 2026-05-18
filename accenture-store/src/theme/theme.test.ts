import { describe, it, expect } from 'vitest';
import { buildTheme } from './theme';

describe('theme/buildTheme', () => {
  it('cria tema light com a paleta correta', () => {
    const t = buildTheme('light');
    expect(t.palette.mode).toBe('light');
    expect(t.palette.primary.main).toBe('#A100FF');
    expect(t.palette.secondary.main).toBe('#7A00CC');
    expect(t.shape.borderRadius).toBe(8);
  });

  it('cria tema dark com a paleta correta', () => {
    const t = buildTheme('dark');
    expect(t.palette.mode).toBe('dark');
    expect(t.palette.primary.main).toBe('#A100FF');
    expect(t.palette.secondary.main).toBe('#B266FF');
  });

  it('aplica typography overrides em h5/h6', () => {
    const t = buildTheme('light');
    expect(t.typography.h5.fontWeight).toBe(600);
    expect(t.typography.h6.fontWeight).toBe(600);
    expect(t.typography.fontFamily).toContain('Roboto');
  });
});
