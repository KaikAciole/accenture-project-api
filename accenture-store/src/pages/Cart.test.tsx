import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../theme/theme';
import { SnackbarProvider } from '../contexts/SnackbarContext';
import Cart from './Cart';

const useAuthMock = vi.fn();
const useCartMock = vi.fn();
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => useAuthMock() }));
vi.mock('../contexts/CartContext', () => ({ useCart: () => useCartMock() }));

function LocationProbe() {
  const l = useLocation();
  return <div data-testid="loc">{l.pathname}</div>;
}

function setup() {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter initialEntries={['/cart']}>
        <SnackbarProvider>
          <Routes>
            <Route path="/cart" element={<Cart />} />
            <Route path="*" element={<LocationProbe />} />
          </Routes>
        </SnackbarProvider>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe('Cart', () => {
  beforeEach(() => {
    useAuthMock.mockReset();
    useCartMock.mockReset();
  });

  it('vazio: mostra mensagem e botão para a loja', async () => {
    useAuthMock.mockReturnValue({ isAuthenticated: true });
    useCartMock.mockReturnValue({ items: [], updateQuantity: vi.fn(), removeItem: vi.fn(), subtotal: 0 });
    const user = userEvent.setup();
    setup();
    expect(screen.getByText('Seu carrinho está vazio.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Ver produtos' }));
    expect(screen.getByTestId('loc')).toHaveTextContent('/');
  });

  it('com itens: lista, soma subtotal e mostra alerta quando não autenticado', () => {
    useAuthMock.mockReturnValue({ isAuthenticated: false });
    useCartMock.mockReturnValue({
      items: [{ sku: 'S', name: 'Camisa', unitPrice: 100, quantity: 2 }],
      updateQuantity: vi.fn(), removeItem: vi.fn(), subtotal: 200,
    });
    setup();
    expect(screen.getByText('Camisa')).toBeInTheDocument();
    expect(screen.getAllByText(/R\$\s?200,00/).length).toBeGreaterThan(0);
    expect(screen.getByText(/Você precisa entrar/)).toBeInTheDocument();
  });

  it('clicar em finalizar quando não autenticado vai para /login', async () => {
    useAuthMock.mockReturnValue({ isAuthenticated: false });
    useCartMock.mockReturnValue({
      items: [{ sku: 'S', name: 'P', unitPrice: 10, quantity: 1 }],
      updateQuantity: vi.fn(), removeItem: vi.fn(), subtotal: 10,
    });
    const user = userEvent.setup();
    setup();
    await user.click(screen.getByRole('button', { name: /finalizar pedido/i }));
    expect(screen.getByTestId('loc')).toHaveTextContent('/login');
  });

  it('finalizar autenticado vai para /checkout', async () => {
    useAuthMock.mockReturnValue({ isAuthenticated: true });
    useCartMock.mockReturnValue({
      items: [{ sku: 'S', name: 'P', unitPrice: 10, quantity: 1 }],
      updateQuantity: vi.fn(), removeItem: vi.fn(), subtotal: 10,
    });
    const user = userEvent.setup();
    setup();
    await user.click(screen.getByRole('button', { name: /finalizar pedido/i }));
    expect(screen.getByTestId('loc')).toHaveTextContent('/checkout');
  });

  it('updateQuantity chamado ao mudar campo numérico', async () => {
    const updateQuantity = vi.fn();
    useAuthMock.mockReturnValue({ isAuthenticated: true });
    useCartMock.mockReturnValue({
      items: [{ sku: 'S', name: 'P', unitPrice: 10, quantity: 1 }],
      updateQuantity, removeItem: vi.fn(), subtotal: 10,
    });
    setup();
    const input = screen.getByRole('spinbutton') as HTMLInputElement;
    const { fireEvent } = await import('@testing-library/react');
    fireEvent.change(input, { target: { value: '5' } });
    expect(updateQuantity).toHaveBeenLastCalledWith('S', 5);
  });

  it('removeItem chamado ao clicar no lixeira', async () => {
    const removeItem = vi.fn();
    useAuthMock.mockReturnValue({ isAuthenticated: true });
    useCartMock.mockReturnValue({
      items: [{ sku: 'S', name: 'P', unitPrice: 10, quantity: 1 }],
      updateQuantity: vi.fn(), removeItem, subtotal: 10,
    });
    const user = userEvent.setup();
    setup();
    await user.click(screen.getByRole('button', { name: /remover/i }));
    expect(removeItem).toHaveBeenCalledWith('S');
  });
});
