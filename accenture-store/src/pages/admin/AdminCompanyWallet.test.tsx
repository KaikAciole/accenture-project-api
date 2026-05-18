import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../../theme/theme';
import AdminCompanyWallet from './AdminCompanyWallet';

vi.mock('../../api/wallets', () => ({
  COMPANY_OWNER_ID: 'company-id',
  getWalletByOwner: vi.fn(),
  listCompanyTransactions: vi.fn(),
}));

import { getWalletByOwner, listCompanyTransactions } from '../../api/wallets';
const walletMock = getWalletByOwner as unknown as ReturnType<typeof vi.fn>;
const txsMock = listCompanyTransactions as unknown as ReturnType<typeof vi.fn>;

function setup() {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter>
        <AdminCompanyWallet />
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe('AdminCompanyWallet', () => {
  beforeEach(() => {
    walletMock.mockReset();
    txsMock.mockReset();
  });

  it('renderiza saldo e transações', async () => {
    walletMock.mockResolvedValue({ balance: 500 });
    txsMock.mockResolvedValue([
      { id: 't1', type: 'CREDIT', reason: 'SALE_RECEIVED', amount: 100, createdAt: '2026-01-01T10:00:00Z' },
      { id: 't2', type: 'DEBIT', reason: 'REFUND', amount: 30 },
    ]);
    setup();
    expect(await screen.findByText(/R\$\s?500,00/)).toBeInTheDocument();
    expect(screen.getByText('Venda')).toBeInTheDocument();
    expect(screen.getByText('Estorno')).toBeInTheDocument();
  });

  it('vazio mostra mensagem', async () => {
    walletMock.mockResolvedValue({ balance: 0 });
    txsMock.mockResolvedValue([]);
    setup();
    expect(await screen.findByText('Sem transações.')).toBeInTheDocument();
  });

  it('aceita Paginated.content em transações', async () => {
    walletMock.mockResolvedValue({ balance: 0 });
    txsMock.mockResolvedValue({ content: [{ id: 't', type: 'CREDIT', reason: 'TOP_UP', amount: 1 }] });
    setup();
    expect(await screen.findByText('Recarga')).toBeInTheDocument();
  });

  it('erro de wallet mostra alert', async () => {
    walletMock.mockRejectedValue(new Error('falhou'));
    txsMock.mockResolvedValue([]);
    setup();
    expect(await screen.findByText('falhou')).toBeInTheDocument();
  });
});
