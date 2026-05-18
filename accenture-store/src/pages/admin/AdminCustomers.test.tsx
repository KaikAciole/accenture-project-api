import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../../theme/theme';
import { SnackbarProvider } from '../../contexts/SnackbarContext';
import AdminCustomers from './AdminCustomers';

vi.mock('../../api/customers', () => ({
  listCustomers: vi.fn(),
  deleteCustomer: vi.fn(),
}));
vi.mock('../../api/wallets', () => ({
  listTransactionsByOwner: vi.fn(),
}));

import { listCustomers, deleteCustomer } from '../../api/customers';
import { listTransactionsByOwner } from '../../api/wallets';

const listMock = listCustomers as unknown as ReturnType<typeof vi.fn>;
const deleteMock = deleteCustomer as unknown as ReturnType<typeof vi.fn>;
const listTxsMock = listTransactionsByOwner as unknown as ReturnType<typeof vi.fn>;

function setup() {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter>
        <SnackbarProvider>
          <AdminCustomers />
        </SnackbarProvider>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe('AdminCustomers', () => {
  beforeEach(() => {
    listMock.mockReset();
    deleteMock.mockReset();
    listTxsMock.mockReset();
  });

  it('renderiza lista de clientes', async () => {
    listMock.mockResolvedValue([
      { id: 'c1', name: 'João', email: 'j@x.c', cpf: '123', phone: '11' },
    ]);
    setup();
    expect(await screen.findByText('João')).toBeInTheDocument();
    expect(screen.getByText('j@x.c')).toBeInTheDocument();
  });

  it('vazio', async () => {
    listMock.mockResolvedValue([]);
    setup();
    expect(await screen.findByText('Sem clientes.')).toBeInTheDocument();
  });

  it('aceita Paginated.content', async () => {
    listMock.mockResolvedValue({ content: [{ id: 'c2', name: 'Maria' }] });
    setup();
    expect(await screen.findByText('Maria')).toBeInTheDocument();
  });

  it('erro mostra alert', async () => {
    listMock.mockRejectedValue(new Error('boom'));
    setup();
    expect(await screen.findByText('boom')).toBeInTheDocument();
  });

  it('abre drawer com extrato e fecha', async () => {
    listMock.mockResolvedValue([{ id: 'c1', name: 'João' }]);
    listTxsMock.mockResolvedValue([
      { id: 't1', type: 'CREDIT', reason: 'TOP_UP', amount: 100 },
    ]);
    const user = userEvent.setup();
    setup();
    await screen.findByText('João');
    await user.click(screen.getByRole('button', { name: /ver extrato/i }));
    expect(await screen.findByText('Recarga')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /fechar/i }));
    await waitFor(() => expect(screen.queryByText('Recarga')).not.toBeInTheDocument());
  });

  it('drawer com erro em transações mostra "Sem transações"', async () => {
    listMock.mockResolvedValue([{ id: 'c1', name: 'João' }]);
    listTxsMock.mockRejectedValue(new Error('x'));
    const user = userEvent.setup();
    setup();
    await screen.findByText('João');
    await user.click(screen.getByRole('button', { name: /ver extrato/i }));
    expect(await screen.findByText('Sem transações.')).toBeInTheDocument();
  });

  it('aceita Paginated.content de transações', async () => {
    listMock.mockResolvedValue([{ id: 'c1', name: 'João' }]);
    listTxsMock.mockResolvedValue({ content: [{ id: 't1', type: 'DEBIT', reason: 'PAYMENT', amount: 50 }] });
    const user = userEvent.setup();
    setup();
    await screen.findByText('João');
    await user.click(screen.getByRole('button', { name: /ver extrato/i }));
    expect(await screen.findByText('Pagamento')).toBeInTheDocument();
  });

  it('excluir cliente: confirma e chama deleteCustomer', async () => {
    listMock.mockResolvedValue([{ id: 'c1', name: 'João' }]);
    deleteMock.mockResolvedValue(undefined);
    const user = userEvent.setup();
    setup();
    await screen.findByText('João');
    await user.click(screen.getByRole('button', { name: /excluir/i }));
    await user.click((await screen.findAllByRole('button', { name: 'Excluir' })).pop()!);
    await waitFor(() => expect(deleteMock).toHaveBeenCalledWith('c1'));
  });

  it('cancelar exclusão fecha sem chamar API', async () => {
    listMock.mockResolvedValue([{ id: 'c1', name: 'João' }]);
    const user = userEvent.setup();
    setup();
    await screen.findByText('João');
    await user.click(screen.getByRole('button', { name: /excluir/i }));
    await user.click(screen.getByRole('button', { name: /cancelar/i }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(deleteMock).not.toHaveBeenCalled();
  });

  it('erro de delete notifica', async () => {
    listMock.mockResolvedValue([{ id: 'c1', name: 'João' }]);
    deleteMock.mockRejectedValue(new Error('falha-del'));
    const user = userEvent.setup();
    setup();
    await screen.findByText('João');
    await user.click(screen.getByRole('button', { name: /excluir/i }));
    await user.click((await screen.findAllByRole('button', { name: 'Excluir' })).pop()!);
    expect(await screen.findByText('falha-del')).toBeInTheDocument();
  });
});
