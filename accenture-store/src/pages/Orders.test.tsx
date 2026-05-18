import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../theme/theme';
import Orders from './Orders';

vi.mock('../api/orders', () => ({ listMyOrders: vi.fn() }));
import { listMyOrders } from '../api/orders';
const listMyOrdersMock = listMyOrders as unknown as ReturnType<typeof vi.fn>;

function setup() {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter>
        <Orders />
      </MemoryRouter>
    </ThemeProvider>,
  );
}

const order = (id: string, status: string, total: number) => ({
  id, orderId: id, status, items: [], totalAmount: total,
  createdAt: '2026-01-01T10:00:00Z',
});

describe('Orders', () => {
  beforeEach(() => listMyOrdersMock.mockReset());

  it('skeleton inicial', () => {
    listMyOrdersMock.mockImplementation(() => new Promise(() => {}));
    setup();
    expect(screen.queryByText('Pedido')).not.toBeInTheDocument();
  });

  it('vazio: mostra estado vazio', async () => {
    listMyOrdersMock.mockResolvedValue([]);
    setup();
    expect(await screen.findByText('Você ainda não fez pedidos')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver produtos' })).toHaveAttribute('href', '/');
  });

  it('renderiza tabela de pedidos com cores por status', async () => {
    listMyOrdersMock.mockResolvedValue([
      order('abc12345-x', 'PAID', 100),
      order('def67890-y', 'PENDING', 50),
    ]);
    setup();
    expect(await screen.findByText('#abc12345')).toBeInTheDocument();
    expect(screen.getByText('#def67890')).toBeInTheDocument();
    expect(screen.getByText('PAID')).toBeInTheDocument();
    expect(screen.getByText('PENDING')).toBeInTheDocument();
  });

  it('aceita Paginated em data/content/items', async () => {
    listMyOrdersMock.mockResolvedValue({ data: [order('id1', 'PAID', 10)] });
    setup();
    expect(await screen.findByText('#id1')).toBeInTheDocument();
  });

  it('erro mostra alert', async () => {
    listMyOrdersMock.mockRejectedValue(new Error('rede'));
    setup();
    expect(await screen.findByText('rede')).toBeInTheDocument();
  });

  it('linka para detalhes do pedido', async () => {
    listMyOrdersMock.mockResolvedValue([order('order123', 'PAID', 99)]);
    setup();
    const detalhes = await screen.findByRole('link', { name: /detalhes/i });
    expect(detalhes).toHaveAttribute('href', '/orders/order123');
  });
});
