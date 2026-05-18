import { describe, it, expect } from 'vitest';
import { renderWithProviders, screen } from '../test/utils';
import Footer from './Footer';

describe('Footer', () => {
  it('renderiza o copyright com o ano corrente', () => {
    renderWithProviders(<Footer />);
    const year = new Date().getFullYear();
    expect(screen.getByText(new RegExp(`© ${year} Acce Store`))).toBeInTheDocument();
  });
});
