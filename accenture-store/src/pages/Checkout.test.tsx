import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../theme/theme';
import { SnackbarProvider } from '../contexts/SnackbarContext';
import Checkout from './Checkout';

const useAuthMock = vi.fn();
const useCartMock = vi.fn();
const clearMock = vi.fn();

vi.mock('../contexts/AuthContext', () => ({ useAuth: () => useAuthMock() }));
vi.mock('../contexts/CartContext', () => ({ useCart: () => useCartMock() }));
vi.mock('../api/customers', () => ({ listAddresses: vi.fn(), createAddress: vi.fn() }));
vi.mock('../api/orders', () => ({ createOrder: vi.fn(), getOrder: vi.fn() }));
vi.mock('../api/payments', () => ({ createPayment: vi.fn(), processPayment: vi.fn() }));
vi.mock('../api/wallets', () => ({ getWalletByOwner: vi.fn() }));
vi.mock('../components/AddressForm', () => ({
  default: ({ onSubmit }: { onSubmit: (a: unknown) => void }) => (
    <button onClick={() => onSubmit({ zipCode: '01000000', street: 'R', number: '1', city: 'SP', state: 'SP' })}>
      form-submit
    </button>
  ),
}));

import { listAddresses, createAddress } from '../api/customers';
import { createOrder, getOrder } from '../api/orders';
import { createPayment, processPayment } from '../api/payments';
import { getWalletByOwner } from '../api/wallets';

const listAddrMock = listAddresses as unknown as ReturnType<typeof vi.fn>;
const createAddrMock = createAddress as unknown as ReturnType<typeof vi.fn>;
const createOrderMock = createOrder as unknown as ReturnType<typeof vi.fn>;
const getOrderMock = getOrder as unknown as ReturnType<typeof vi.fn>;
const createPaymentMock = createPayment as unknown as ReturnType<typeof vi.fn>;
const processPaymentMock = processPayment as unknown as ReturnType<typeof vi.fn>;
const getWalletMock = getWalletByOwner as unknown as ReturnType<typeof vi.fn>;

function LocationProbe() {
  const l = useLocation();
  return <div data-testid="loc">{l.pathname}</div>;
}

function setup() {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter initialEntries={['/checkout']}>
        <SnackbarProvider>
          <Routes>
            <Route path="/checkout" element={<Checkout />} />
            <Route path="*" element={<LocationProbe />} />
          </Routes>
        </SnackbarProvider>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

const addr = (id: string) => ({
  id, zipCode: '01000-000', street: 'R A', number: '10', neighborhood: 'B', city: 'SP', state: 'SP',
});

describe('Checkout', () => {
  beforeEach(() => {
    [useAuthMock, useCartMock, clearMock, listAddrMock, createAddrMock,
     createOrderMock, getOrderMock, createPaymentMock, processPaymentMock, getWalletMock]
      .forEach((m) => m.mockReset());
    useAuthMock.mockReturnValue({ user: { customerId: 'c1', email: 'e@x.c' } });
    useCartMock.mockReturnValue({
      items: [{ sku: 's1', name: 'P', unitPrice: 50, quantity: 2 }],
      subtotal: 100,
      clear: clearMock,
    });
    listAddrMock.mockResolvedValue([]);
    getWalletMock.mockResolvedValue({ balance: 200 });
  });

  it('redireciona para /cart quando carrinho vazio', () => {
    useCartMock.mockReturnValue({ items: [], subtotal: 0, clear: clearMock });
    setup();
    expect(screen.getByTestId('loc')).toHaveTextContent('/cart');
  });

  it('skeleton enquanto carrega endereços', () => {
    listAddrMock.mockImplementation(() => new Promise(() => {}));
    setup();
    expect(screen.queryByText('Checkout')).not.toBeInTheDocument();
  });

  it('sem endereços mostra formulário inline', async () => {
    listAddrMock.mockResolvedValue([]);
    setup();
    expect(await screen.findByText(/Cadastre um endereço/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'form-submit' })).toBeInTheDocument();
  });

  it('cria endereço inline e usa-o', async () => {
    listAddrMock.mockResolvedValue([]);
    createAddrMock.mockResolvedValue(addr('a-new'));
    const user = userEvent.setup();
    setup();
    await screen.findByText(/Cadastre um endereço/i);
    await user.click(screen.getByRole('button', { name: 'form-submit' }));
    await waitFor(() => expect(createAddrMock).toHaveBeenCalled());
  });

  it('com endereços: lista, seleciona o primeiro e mostra Resumo', async () => {
    listAddrMock.mockResolvedValue([addr('a1')]);
    setup();
    expect(await screen.findByText('Checkout')).toBeInTheDocument();
    expect(screen.getByText('P × 2')).toBeInTheDocument();
    expect(screen.getAllByText(/R\$\s?100,00/).length).toBeGreaterThan(0);
  });

  it('saldo insuficiente mostra alert e desabilita confirmar', async () => {
    getWalletMock.mockResolvedValue({ balance: 5 });
    listAddrMock.mockResolvedValue([addr('a1')]);
    setup();
    expect(await screen.findByText(/Saldo insuficiente/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /confirmar e pagar/i })).toBeDisabled();
  });

  it('confirma pedido completo: reserva → paga → navega para detalhe', async () => {
    listAddrMock.mockResolvedValue([addr('a1')]);
    createOrderMock.mockResolvedValue({ id: 'o1' });
    getOrderMock.mockResolvedValue({ id: 'o1', status: 'RESERVED', totalAmount: 100 });
    createPaymentMock.mockResolvedValue({ id: 'p1' });
    processPaymentMock.mockResolvedValue({});

    const user = userEvent.setup();
    setup();
    await screen.findByText('Checkout');
    await user.click(screen.getByRole('button', { name: /confirmar e pagar/i }));

    await waitFor(() => expect(createOrderMock).toHaveBeenCalled(), { timeout: 5000 });
    await waitFor(() => expect(createPaymentMock).toHaveBeenCalled(), { timeout: 5000 });
    await waitFor(() => expect(processPaymentMock).toHaveBeenCalledWith('p1'), { timeout: 5000 });
    expect(clearMock).toHaveBeenCalled();
    await waitFor(() => expect(screen.getByTestId('loc')).toHaveTextContent('/orders/o1'));
  });

  it('pedido cancelado durante polling lança erro com message', async () => {
    listAddrMock.mockResolvedValue([addr('a1')]);
    createOrderMock.mockResolvedValue({ id: 'o1' });
    getOrderMock.mockResolvedValue({ id: 'o1', status: 'CANCELED' });

    const user = userEvent.setup();
    setup();
    await screen.findByText('Checkout');
    await user.click(screen.getByRole('button', { name: /confirmar e pagar/i }));
    await waitFor(() => expect(screen.getByText('Pedido cancelado')).toBeInTheDocument(), { timeout: 5000 });
  });

  it('pedido criado sem ID dispara erro', async () => {
    listAddrMock.mockResolvedValue([addr('a1')]);
    createOrderMock.mockResolvedValue({});

    const user = userEvent.setup();
    setup();
    await screen.findByText('Checkout');
    await user.click(screen.getByRole('button', { name: /confirmar e pagar/i }));
    await waitFor(() => expect(screen.getByText('Pedido criado sem ID')).toBeInTheDocument());
  });

  it('erro na listagem notifica', async () => {
    listAddrMock.mockRejectedValue(new Error('boom'));
    setup();
    expect(await screen.findByText('boom')).toBeInTheDocument();
  });
});
