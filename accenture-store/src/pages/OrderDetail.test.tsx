import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../theme/theme';
import { SnackbarProvider } from '../contexts/SnackbarContext';
import OrderDetail from './OrderDetail';

vi.mock('../api/orders', () => ({ getOrder: vi.fn(), cancelOrder: vi.fn() }));
vi.mock('../api/payments', () => ({ getPaymentByOrder: vi.fn() }));

import { getOrder, cancelOrder } from '../api/orders';
import { getPaymentByOrder } from '../api/payments';

const getOrderMock = getOrder as unknown as ReturnType<typeof vi.fn>;
const cancelOrderMock = cancelOrder as unknown as ReturnType<typeof vi.fn>;
const getPaymentByOrderMock = getPaymentByOrder as unknown as ReturnType<typeof vi.fn>;

function LocationProbe() {
  const l = useLocation();
  return <div data-testid="loc">{l.pathname}</div>;
}

function setup(id = 'o1') {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter initialEntries={[`/orders/${id}`]}>
        <SnackbarProvider>
          <Routes>
            <Route path="/orders/:id" element={<OrderDetail />} />
            <Route path="*" element={<LocationProbe />} />
          </Routes>
        </SnackbarProvider>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

const baseOrder = {
  id: 'o1', status: 'PAID', totalAmount: 100,
  items: [{ sku: 's1', name: 'P1', quantity: 2, unitPrice: 50 }],
  deliveryAddress: { street: 'R A', number: '1', city: 'SP', state: 'SP', neighborhood: 'B', zipCode: '01000-000' },
  createdAt: '2026-01-01T10:00:00Z',
};

describe('OrderDetail', () => {
  beforeEach(() => {
    getOrderMock.mockReset();
    cancelOrderMock.mockReset();
    getPaymentByOrderMock.mockReset();
    getPaymentByOrderMock.mockResolvedValue(null);
  });

  it('skeleton inicial', () => {
    getOrderMock.mockImplementation(() => new Promise(() => {}));
    setup();
    expect(screen.queryByText(/Pedido #/)).not.toBeInTheDocument();
  });

  it('mostra erro quando getOrder falha', async () => {
    getOrderMock.mockRejectedValue(new Error('boom'));
    setup();
    expect(await screen.findByText('boom')).toBeInTheDocument();
  });

  it('renderiza pedido completo com endereço e pagamento aprovado', async () => {
    getOrderMock.mockResolvedValue(baseOrder);
    getPaymentByOrderMock.mockResolvedValue({ id: 'p1', status: 'APPROVED', method: 'WALLET', amount: 100 });
    setup();
    expect(await screen.findByText('Pedido #o1')).toBeInTheDocument();
    expect(screen.getByText('P1 × 2')).toBeInTheDocument();
    expect(screen.getByText('PAID')).toBeInTheDocument();
    expect(screen.getByText('APPROVED')).toBeInTheDocument();
    expect(screen.getByText('Carteira')).toBeInTheDocument();
    expect(screen.getByText(/R A, 1/)).toBeInTheDocument();
  });

  it('sem payment mostra aguardando pagamento', async () => {
    getOrderMock.mockResolvedValue(baseOrder);
    getPaymentByOrderMock.mockRejectedValue(new Error('no payment'));
    setup();
    await screen.findByText('Pedido #o1');
    expect(await screen.findByText('Aguardando pagamento.')).toBeInTheDocument();
  });

  it('botão Voltar navega para /orders', async () => {
    getOrderMock.mockResolvedValue(baseOrder);
    const user = userEvent.setup();
    setup();
    await screen.findByText('Pedido #o1');
    await user.click(screen.getByRole('button', { name: 'Voltar' }));
    expect(screen.getByTestId('loc')).toHaveTextContent('/orders');
  });

  it('status PAID permite cancelar (abre diálogo)', async () => {
    getOrderMock.mockResolvedValue(baseOrder);
    const user = userEvent.setup();
    setup();
    await screen.findByText('Pedido #o1');
    await user.click(screen.getByRole('button', { name: /cancelar pedido/i }));
    expect(await screen.findByText(/Tem certeza/)).toBeInTheDocument();
  });

  it('status CANCELED não mostra botão cancelar', async () => {
    getOrderMock.mockResolvedValue({ ...baseOrder, status: 'CANCELED' });
    setup();
    await screen.findByText('Pedido #o1');
    expect(screen.queryByRole('button', { name: /cancelar pedido/i })).not.toBeInTheDocument();
  });

  it('erro no cancel notifica e mantém aberto', async () => {
    getOrderMock.mockResolvedValue(baseOrder);
    cancelOrderMock.mockRejectedValue(new Error('falha cancel'));
    const user = userEvent.setup();
    setup();
    await screen.findByText('Pedido #o1');
    await user.click(screen.getByRole('button', { name: /cancelar pedido/i }));
    const dialog = await screen.findByRole('dialog');
    const confirmBtn = (await screen.findAllByRole('button', { name: /cancelar pedido/i })).find((b) => dialog.contains(b));
    await user.click(confirmBtn!);
    await waitFor(() => expect(cancelOrderMock).toHaveBeenCalled());
  });

  it('diálogo botão "Voltar" fecha sem cancelar', async () => {
    getOrderMock.mockResolvedValue(baseOrder);
    const user = userEvent.setup();
    setup();
    await screen.findByText('Pedido #o1');
    await user.click(screen.getByRole('button', { name: /cancelar pedido/i }));
    const voltarBtn = (await screen.findAllByRole('button', { name: /^voltar$/i })).pop();
    await user.click(voltarBtn!);
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(cancelOrderMock).not.toHaveBeenCalled();
  });
});
