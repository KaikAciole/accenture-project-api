import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../../theme/theme';
import AdminOrders from './AdminOrders';

vi.mock('../../api/orders', () => ({ listAllOrders: vi.fn() }));
import { listAllOrders } from '../../api/orders';
const mock = listAllOrders as unknown as ReturnType<typeof vi.fn>;

function setup() {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter>
        <AdminOrders />
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe('AdminOrders', () => {
  beforeEach(() => mock.mockReset());

  it('mostra lista de pedidos com customerId e status', async () => {
    mock.mockResolvedValue([
      { id: 'o1', customerId: 'c1', status: 'PAID', totalAmount: 100, createdAt: '2026-01-01T10:00:00Z', items: [] },
    ]);
    setup();
    expect(await screen.findByText('o1')).toBeInTheDocument();
    expect(screen.getByText('c1')).toBeInTheDocument();
    expect(screen.getByText('PAID')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /detalhes/i })).toHaveAttribute('href', '/orders/o1');
  });

  it('estado vazio', async () => {
    mock.mockResolvedValue([]);
    setup();
    expect(await screen.findByText('Nenhum pedido.')).toBeInTheDocument();
  });

  it('aceita Paginated.content', async () => {
    mock.mockResolvedValue({ content: [{ id: 'o9', customerId: 'c9', status: 'PENDING', items: [] }] });
    setup();
    expect(await screen.findByText('o9')).toBeInTheDocument();
  });

  it('erro mostra alert', async () => {
    mock.mockRejectedValue(new Error('falhou'));
    setup();
    expect(await screen.findByText('falhou')).toBeInTheDocument();
  });
});
