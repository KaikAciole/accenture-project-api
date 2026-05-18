import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../theme/theme';
import { SnackbarProvider } from '../contexts/SnackbarContext';
import Wallet from './Wallet';

const useAuthMock = vi.fn();
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => useAuthMock() }));
vi.mock('../api/wallets', () => ({
  getWalletByOwner: vi.fn(),
  listTransactionsByOwner: vi.fn(),
  createTopUp: vi.fn(),
  submitTopUp: vi.fn(),
}));

import {
  getWalletByOwner, listTransactionsByOwner, createTopUp, submitTopUp,
} from '../api/wallets';

const getWalletMock = getWalletByOwner as unknown as ReturnType<typeof vi.fn>;
const listTxsMock = listTransactionsByOwner as unknown as ReturnType<typeof vi.fn>;
const createTopUpMock = createTopUp as unknown as ReturnType<typeof vi.fn>;
const submitTopUpMock = submitTopUp as unknown as ReturnType<typeof vi.fn>;

function setup() {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter>
        <SnackbarProvider>
          <Wallet />
        </SnackbarProvider>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe('Wallet', () => {
  beforeEach(() => {
    useAuthMock.mockReset();
    getWalletMock.mockReset();
    listTxsMock.mockReset();
    createTopUpMock.mockReset();
    submitTopUpMock.mockReset();
    useAuthMock.mockReturnValue({ user: { customerId: 'c1', email: 'e@x.c' } });
  });

  it('skeleton enquanto carrega', () => {
    getWalletMock.mockImplementation(() => new Promise(() => {}));
    listTxsMock.mockResolvedValue([]);
    setup();
    expect(screen.queryByText('Saldo da carteira')).not.toBeInTheDocument();
  });

  it('exibe erro quando falha', async () => {
    getWalletMock.mockRejectedValue(new Error('rede'));
    listTxsMock.mockResolvedValue([]);
    setup();
    expect(await screen.findByText('rede')).toBeInTheDocument();
  });

  it('exibe saldo e tabela vazia', async () => {
    getWalletMock.mockResolvedValue({ id: 'w1', balance: 250.5 });
    listTxsMock.mockResolvedValue([]);
    setup();
    expect(await screen.findByText('Saldo da carteira')).toBeInTheDocument();
    expect(screen.getByText(/R\$\s?250,50/)).toBeInTheDocument();
    expect(screen.getByText('Sem transações.')).toBeInTheDocument();
  });

  it('renderiza transações com tipos crédito e débito', async () => {
    getWalletMock.mockResolvedValue({ id: 'w1', balance: 100 });
    listTxsMock.mockResolvedValue([
      { id: 't1', type: 'CREDIT', reason: 'TOP_UP', amount: 50, createdAt: '2026-01-01T10:00:00Z' },
      { id: 't2', type: 'DEBIT', reason: 'PAYMENT', amount: 20, createdAt: '2026-01-02T10:00:00Z' },
    ]);
    setup();
    expect(await screen.findByText('Crédito')).toBeInTheDocument();
    expect(screen.getByText('Débito')).toBeInTheDocument();
    expect(screen.getByText('Recarga')).toBeInTheDocument();
    expect(screen.getByText('Pagamento')).toBeInTheDocument();
  });

  it('abre diálogo de recarga e valida valor zero', async () => {
    getWalletMock.mockResolvedValue({ id: 'w1', balance: 0 });
    listTxsMock.mockResolvedValue([]);
    const user = userEvent.setup();
    setup();
    await screen.findByText('Saldo da carteira');
    await user.click(screen.getByRole('button', { name: /recarregar saldo/i }));
    await user.click(screen.getByRole('button', { name: /gerar qr code/i }));
    expect(await screen.findByText('Informe um valor maior que zero')).toBeInTheDocument();
  });

  it('gera QR Code Pix e mostra textarea + botão copiar', async () => {
    getWalletMock.mockResolvedValue({ id: 'w1', balance: 10 });
    listTxsMock.mockResolvedValue([]);
    createTopUpMock.mockResolvedValue({ id: 't1' });
    submitTopUpMock.mockResolvedValue({ qrCode: 'PIXCODE', qrCodeBase64: 'AAA' });

    const user = userEvent.setup();
    setup();
    await screen.findByText('Saldo da carteira');
    await user.click(screen.getByRole('button', { name: /recarregar saldo/i }));
    await user.type(screen.getByLabelText(/valor/i), '100');
    await user.click(screen.getByRole('button', { name: /gerar qr code/i }));

    expect(await screen.findByText(/Escaneie o QR Code/)).toBeInTheDocument();
    expect(createTopUpMock).toHaveBeenCalledWith('w1', { customerId: 'c1', amount: 100, customerEmail: 'e@x.c' });
    expect(submitTopUpMock).toHaveBeenCalledWith('t1');
  });

  it('botão de copiar Pix invoca o handler', async () => {
    getWalletMock.mockResolvedValue({ id: 'w1', balance: 10 });
    listTxsMock.mockResolvedValue([]);
    createTopUpMock.mockResolvedValue({ id: 't1' });
    submitTopUpMock.mockResolvedValue({ qrCode: 'PIX123', qrCodeBase64: 'AAA' });

    const user = userEvent.setup();
    setup();
    await screen.findByText('Saldo da carteira');
    await user.click(screen.getByRole('button', { name: /recarregar saldo/i }));
    await user.type(screen.getByLabelText(/valor/i), '50');
    await user.click(screen.getByRole('button', { name: /gerar qr code/i }));
    await screen.findByText(/Escaneie o QR Code/);
    const copyBtn = screen.getByRole('button', { name: /copiar código pix/i });
    await user.click(copyBtn);
    await waitFor(() =>
      expect(
        screen.queryByText(/Codigo Pix copiado/i) || screen.queryByText(/Nao foi possivel copiar/i),
      ).toBeInTheDocument(),
    );
  });

  it('falha ao gerar QR notifica erro', async () => {
    getWalletMock.mockResolvedValue({ id: 'w1', balance: 0 });
    listTxsMock.mockResolvedValue([]);
    createTopUpMock.mockRejectedValue(new Error('erro pix'));
    const user = userEvent.setup();
    setup();
    await screen.findByText('Saldo da carteira');
    await user.click(screen.getByRole('button', { name: /recarregar saldo/i }));
    await user.type(screen.getByLabelText(/valor/i), '50');
    await user.click(screen.getByRole('button', { name: /gerar qr code/i }));
    await waitFor(() => expect(screen.getByText('erro pix')).toBeInTheDocument());
  });

  it('fechar diálogo limpa estado', async () => {
    getWalletMock.mockResolvedValue({ id: 'w1', balance: 0 });
    listTxsMock.mockResolvedValue([]);
    const user = userEvent.setup();
    setup();
    await screen.findByText('Saldo da carteira');
    await user.click(screen.getByRole('button', { name: /recarregar saldo/i }));
    await user.click(screen.getByRole('button', { name: 'Fechar' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('aceita Paginated.content em transações', async () => {
    getWalletMock.mockResolvedValue({ id: 'w1', balance: 0 });
    listTxsMock.mockResolvedValue({ content: [{ id: 't', type: 'CREDIT', reason: 'TOP_UP', amount: 1 }] });
    setup();
    expect(await screen.findByText('Recarga')).toBeInTheDocument();
  });
});
