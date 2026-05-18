import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../../theme/theme';
import AdminDashboard from './AdminDashboard';

vi.mock('@mui/x-charts/LineChart', () => ({
  LineChart: (props: { series?: { id?: string; label?: string; data?: number[] }[] }) => (
    <div data-testid="line-chart">
      {(props.series || []).map((s) => (
        <div key={s.id || s.label} data-testid={`line-series-${s.id || s.label}`}>
          {(s.data || []).join(',')}
        </div>
      ))}
    </div>
  ),
}));

vi.mock('@mui/x-charts/PieChart', () => ({
  PieChart: (props: { series?: { data?: { label?: string; value?: number }[] }[] }) => (
    <div data-testid="pie-chart">
      {(props.series?.[0]?.data || []).map((d, i) => (
        <div key={i} data-testid={`pie-slice-${d.label}`}>{d.label}:{d.value}</div>
      ))}
    </div>
  ),
}));

vi.mock('../../api/orders', () => ({ listAllOrders: vi.fn() }));
vi.mock('../../api/wallets', () => ({
  COMPANY_OWNER_ID: 'company-id',
  getWalletByOwner: vi.fn(),
}));

import { listAllOrders } from '../../api/orders';
import { getWalletByOwner } from '../../api/wallets';

const listAllOrdersMock = listAllOrders as unknown as ReturnType<typeof vi.fn>;
const getWalletMock = getWalletByOwner as unknown as ReturnType<typeof vi.fn>;

const daysAgo = (n: number) => new Date(Date.now() - n * 86_400_000).toISOString();

function makeOrders() {
  return [
    { id: '1', customerId: 'c', status: 'PAID', total: 100, createdAt: daysAgo(2),
      items: [{ sku: 'SKU-A', name: 'Camisa', quantity: 2, unitPrice: 50 }] },
    { id: '2', customerId: 'c', status: 'PAID', total: 200, createdAt: daysAgo(2),
      items: [{ sku: 'SKU-B', name: 'Caneca', quantity: 3, unitPrice: 50 }] },
    { id: '3', customerId: 'c', status: 'CANCELED', total: 50, createdAt: daysAgo(5),
      items: [{ sku: 'SKU-C', name: 'Bone', quantity: 1, unitPrice: 50 }] },
    { id: '4', customerId: 'c', status: 'PENDING', total: 80, createdAt: daysAgo(10),
      items: [] },
    { id: '5', customerId: 'c', status: 'PAID', total: 500, createdAt: daysAgo(45),
      items: [{ sku: 'SKU-D', name: 'Antigo', quantity: 1, unitPrice: 500 }] },
  ];
}

function renderDashboard() {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter>
        <AdminDashboard />
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe('AdminDashboard', () => {
  beforeEach(() => {
    listAllOrdersMock.mockReset();
    getWalletMock.mockReset();

    listAllOrdersMock.mockResolvedValue(makeOrders());
    getWalletMock.mockResolvedValue({ balance: 9999.5 });
  });

  it('renderiza skeletons enquanto carrega', () => {
    listAllOrdersMock.mockImplementation(() => new Promise(() => {}));
    getWalletMock.mockImplementation(() => new Promise(() => {}));

    const { container } = renderDashboard();
    expect(container.querySelectorAll('.MuiSkeleton-root').length).toBeGreaterThan(0);
  });

  it('busca pedidos e saldo na montagem', async () => {
    renderDashboard();
    await waitFor(() => expect(listAllOrdersMock).toHaveBeenCalledWith(0, 200));
    expect(getWalletMock).toHaveBeenCalledWith('company-id', 'COMPANY');
  });

  it('mostra cards com totais filtrados (30 dias por padrão)', async () => {
    renderDashboard();
    await screen.findByText('4');
    expect(screen.getByText('R$ 150,00')).toBeInTheDocument();
    expect(screen.getByText('R$ 9.999,50')).toBeInTheDocument();
  });

  it('renderiza os 2 gráficos quando há dados', async () => {
    renderDashboard();
    expect(await screen.findByTestId('line-chart')).toBeInTheDocument();
    expect(screen.getByTestId('pie-chart')).toBeInTheDocument();
  });

  it('pie chart agrupa por status dos pedidos do período', async () => {
    renderDashboard();
    expect(await screen.findByTestId('pie-slice-PAID')).toHaveTextContent('PAID:2');
    expect(screen.getByTestId('pie-slice-CANCELED')).toHaveTextContent('CANCELED:1');
    expect(screen.getByTestId('pie-slice-PENDING')).toHaveTextContent('PENDING:1');
  });

  it('filtro 7 dias reduz o conjunto', async () => {
    const user = userEvent.setup();
    renderDashboard();
    await screen.findByText('4');
    await user.click(screen.getByRole('button', { name: 'período 7 dias' }));
    await waitFor(() => expect(screen.getByText('3')).toBeInTheDocument());
  });

  it('filtro Tudo inclui pedido antigo (45 dias atrás)', async () => {
    const user = userEvent.setup();
    renderDashboard();
    await screen.findByText('4');
    await user.click(screen.getByRole('button', { name: 'período Tudo' }));
    await waitFor(() => expect(screen.getByText('5')).toBeInTheDocument());
    expect(screen.getByText('R$ 266,67')).toBeInTheDocument();
  });

  it('quando não há pedidos PAID no período, mostra mensagem no gráfico de vendas', async () => {
    listAllOrdersMock.mockResolvedValue([
      { id: 'x', customerId: 'c', status: 'CANCELED', items: [], createdAt: daysAgo(1), total: 10 },
    ]);
    renderDashboard();
    expect(await screen.findByText('Sem pedidos pagos no período')).toBeInTheDocument();
  });

  it('quando não há pedidos: pie chart mostra mensagem', async () => {
    listAllOrdersMock.mockResolvedValue([]);
    renderDashboard();
    await waitFor(() => expect(screen.getByText('Sem pedidos no período')).toBeInTheDocument());
  });

  it('aceita resposta paginada (content)', async () => {
    listAllOrdersMock.mockResolvedValue({ content: makeOrders() });
    renderDashboard();
    await screen.findByText('4');
  });

  it('aceita resposta paginada (items)', async () => {
    listAllOrdersMock.mockResolvedValue({ items: makeOrders() });
    renderDashboard();
    await screen.findByText('4');
  });

  it('falha do getWallet zera o saldo sem quebrar', async () => {
    getWalletMock.mockRejectedValue(new Error('boom'));
    renderDashboard();
    await screen.findByText('4');
    expect(screen.getByText('R$ 0,00')).toBeInTheDocument();
  });

  it('pedido PAID sem createdAt é ignorado na série temporal mas conta nos totais (Tudo)', async () => {
    listAllOrdersMock.mockResolvedValue([
      { id: 'a', customerId: 'c', status: 'PAID', total: 100, items: [], createdAt: undefined },
      { id: 'b', customerId: 'c', status: 'PAID', total: 200, createdAt: daysAgo(1), items: [] },
    ]);
    const user = userEvent.setup();
    renderDashboard();
    await waitFor(() => expect(listAllOrdersMock).toHaveBeenCalled());
    await user.click(screen.getByRole('button', { name: 'período Tudo' }));
    await waitFor(() => expect(screen.getByText('2')).toBeInTheDocument());
    const lineSeries = await screen.findByTestId('line-series-revenue');
    expect(lineSeries.textContent?.split(',').length).toBe(1);
  });
});
