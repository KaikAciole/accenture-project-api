import { describe, it, expect } from 'vitest';
import { renderWithProviders, screen, fireEvent } from '../test/utils';
import MinimalHeader from './MinimalHeader';

describe('MinimalHeader', () => {
  it('renderiza logo, marca e botão Entrar', () => {
    renderWithProviders(<MinimalHeader />);
    expect(screen.getByAltText('acce store')).toBeInTheDocument();
    expect(screen.getByText(/acce/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /entrar/i })).toHaveAttribute('href', '/login');
  });

  it('esconde a imagem do logo no error handler', () => {
    renderWithProviders(<MinimalHeader />);
    const img = screen.getByAltText('acce store') as HTMLImageElement;
    fireEvent.error(img);
    expect(img.style.display).toBe('none');
  });
});
