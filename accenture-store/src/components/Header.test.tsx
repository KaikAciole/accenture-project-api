import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../theme/theme';
import Header from './Header';

const useAuthMock = vi.fn();
const useCartMock = vi.fn();

vi.mock('../contexts/AuthContext', () => ({ useAuth: () => useAuthMock() }));
vi.mock('../contexts/CartContext', () => ({ useCart: () => useCartMock() }));
vi.mock('./WalletBadge', () => ({ default: () => null }));
vi.mock('./ThemeToggleButton', () => ({ default: () => null }));

function LocationProbe() {
  const loc = useLocation();
  return <div data-testid="loc">{loc.pathname}{loc.search}</div>;
}

function setup(initial = '/') {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter initialEntries={[initial]}>
        <Header />
        <Routes>
          <Route path="*" element={<LocationProbe />} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe('Header', () => {
  beforeEach(() => {
    useAuthMock.mockReset();
    useCartMock.mockReset();
    useCartMock.mockReturnValue({ totalItems: 0 });
  });

  it('não autenticado: mostra botão Entrar', () => {
    useAuthMock.mockReturnValue({ user: null, isAuthenticated: false, isAdmin: false, logout: vi.fn() });
    setup();
    expect(screen.getByRole('link', { name: /entrar/i })).toHaveAttribute('href', '/login');
  });

  it('autenticado mostra saudação "Olá, {primeiro nome}"', () => {
    useAuthMock.mockReturnValue({
      user: { name: 'João Silva', email: 'j@b.c' },
      isAuthenticated: true,
      isAdmin: false,
      logout: vi.fn(),
    });
    setup();
    expect(screen.getByText(/Olá,\s*João/i)).toBeInTheDocument();
  });

  it('busca: submit redireciona com query encoded', async () => {
    useAuthMock.mockReturnValue({ user: null, isAuthenticated: false, isAdmin: false, logout: vi.fn() });
    const user = userEvent.setup();
    setup();
    const input = screen.getByPlaceholderText('Buscar produtos…');
    await user.type(input, 'café especial');
    await user.click(screen.getByRole('button', { name: /buscar$/i }));
    expect(screen.getByTestId('loc').textContent).toBe('/?q=caf%C3%A9%20especial');
  });

  it('admin: oculta busca e carrinho, link do logo aponta para /admin', () => {
    useAuthMock.mockReturnValue({
      user: { name: 'Adm', roles: ['ADMIN'] },
      isAuthenticated: true,
      isAdmin: true,
      logout: vi.fn(),
    });
    setup();
    expect(screen.queryByPlaceholderText('Buscar produtos…')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('carrinho')).not.toBeInTheDocument();
    expect(screen.getByAltText('acce store').closest('a')).toHaveAttribute('href', '/admin');
  });

  it('carrinho mostra badge com totalItems', () => {
    useAuthMock.mockReturnValue({ user: null, isAuthenticated: false, isAdmin: false, logout: vi.fn() });
    useCartMock.mockReturnValue({ totalItems: 7 });
    setup();
    expect(screen.getByText('7')).toBeInTheDocument();
  });

  it('menu do usuário abre, oferece Sair e desloga', async () => {
    const logout = vi.fn();
    useAuthMock.mockReturnValue({
      user: { name: 'Ana Maria', email: 'a@b.c' },
      isAuthenticated: true,
      isAdmin: false,
      logout,
    });
    const user = userEvent.setup();
    setup('/products/1');

    await user.click(screen.getByRole('button', { name: /Olá,/i }));
    expect(await screen.findByText('Meu perfil')).toBeInTheDocument();
    expect(screen.getByText('Meus pedidos')).toBeInTheDocument();
    expect(screen.getByText('Carteira')).toBeInTheDocument();
    expect(screen.getByText('Endereços')).toBeInTheDocument();

    await user.click(screen.getByText('Sair'));
    expect(logout).toHaveBeenCalled();
    expect(screen.getByTestId('loc').textContent).toBe('/login');
  });

  it('admin: menu mostra "Painel admin"', async () => {
    useAuthMock.mockReturnValue({
      user: { name: 'Adm', email: 'a@b.c' },
      isAuthenticated: true,
      isAdmin: true,
      logout: vi.fn(),
    });
    const user = userEvent.setup();
    setup();
    await user.click(screen.getByRole('button', { name: /Olá,/i }));
    expect(await screen.findByText('Painel admin')).toBeInTheDocument();
    expect(screen.queryByText('Meu perfil')).not.toBeInTheDocument();
  });

  it('erro no logo esconde a imagem', () => {
    useAuthMock.mockReturnValue({ user: null, isAuthenticated: false, isAdmin: false, logout: vi.fn() });
    setup();
    const img = screen.getByAltText('acce store') as HTMLImageElement;
    fireEvent.error(img);
    expect(img.style.display).toBe('none');
  });

  it('inicializa input com query da URL', () => {
    useAuthMock.mockReturnValue({ user: null, isAuthenticated: false, isAdmin: false, logout: vi.fn() });
    setup('/?q=teclado');
    expect((screen.getByPlaceholderText('Buscar produtos…') as HTMLInputElement).value).toBe('teclado');
  });

  it('user sem nome: fallback "Você"', () => {
    useAuthMock.mockReturnValue({
      user: { name: '', email: '' },
      isAuthenticated: true,
      isAdmin: false,
      logout: vi.fn(),
    });
    setup();
    expect(screen.getByText(/Olá,\s*Você/)).toBeInTheDocument();
  });
});
