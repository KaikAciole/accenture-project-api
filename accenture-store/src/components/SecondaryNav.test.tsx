import { describe, it, expect } from 'vitest';
import { renderWithProviders, screen } from '../test/utils';
import SecondaryNav from './SecondaryNav';

describe('SecondaryNav', () => {
  it('renderiza todos os links fixos com hrefs corretos', () => {
    renderWithProviders(<SecondaryNav />);
    const expected = [
      ['Todos os produtos', '/'],
      ['Meus pedidos', '/orders'],
      ['Endereços', '/addresses'],
      ['Carteira', '/wallet'],
    ] as const;
    for (const [label, href] of expected) {
      expect(screen.getByRole('link', { name: label })).toHaveAttribute('href', href);
    }
  });
});
