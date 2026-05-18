import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../theme/theme';
import { SnackbarProvider } from '../contexts/SnackbarContext';
import Addresses from './Addresses';

const useAuthMock = vi.fn();
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => useAuthMock() }));
vi.mock('../api/customers', () => ({
  listAddresses: vi.fn(),
  createAddress: vi.fn(),
  updateAddress: vi.fn(),
  deleteAddress: vi.fn(),
}));
vi.mock('../components/AddressForm', () => ({
  default: ({ onSubmit, onCancel, initial }: { onSubmit: (a: unknown) => void; onCancel?: () => void; initial?: { number?: string } }) => (
    <div>
      <span data-testid="form-initial">{initial?.number || ''}</span>
      <button onClick={() => onSubmit({ zipCode: '01000000', street: 'R', number: '1', city: 'SP', state: 'SP' })}>
        form-submit
      </button>
      {onCancel && <button onClick={onCancel}>form-cancel</button>}
    </div>
  ),
}));

import { listAddresses, createAddress, updateAddress, deleteAddress } from '../api/customers';

const listMock = listAddresses as unknown as ReturnType<typeof vi.fn>;
const createMock = createAddress as unknown as ReturnType<typeof vi.fn>;
const updateMock = updateAddress as unknown as ReturnType<typeof vi.fn>;
const deleteMock = deleteAddress as unknown as ReturnType<typeof vi.fn>;

function setup() {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter>
        <SnackbarProvider>
          <Addresses />
        </SnackbarProvider>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

const addr = (id: string, number = '10') => ({
  id, zipCode: '01000000', street: 'R A', number,
  neighborhood: 'B', city: 'SP', state: 'SP',
});

describe('Addresses', () => {
  beforeEach(() => {
    useAuthMock.mockReset();
    listMock.mockReset();
    createMock.mockReset();
    updateMock.mockReset();
    deleteMock.mockReset();
    useAuthMock.mockReturnValue({ user: { customerId: 'c1' } });
  });

  it('skeleton inicial', () => {
    listMock.mockImplementation(() => new Promise(() => {}));
    setup();
    expect(screen.queryByText('Nenhum endereço cadastrado')).not.toBeInTheDocument();
  });

  it('vazio mostra empty state com botão adicionar', async () => {
    listMock.mockResolvedValue([]);
    setup();
    expect(await screen.findByText('Nenhum endereço cadastrado')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /adicionar endereço/i }).length).toBeGreaterThan(0);
  });

  it('renderiza lista de endereços', async () => {
    listMock.mockResolvedValue([addr('a1'), addr('a2', '20')]);
    setup();
    const numbers = await screen.findAllByText(/R A,/);
    expect(numbers).toHaveLength(2);
  });

  it('aceita Paginated.content', async () => {
    listMock.mockResolvedValue({ content: [addr('a1')] });
    setup();
    expect(await screen.findByText(/R A,/)).toBeInTheDocument();
  });

  it('erro de list mostra alert', async () => {
    listMock.mockRejectedValue(new Error('falhou'));
    setup();
    expect(await screen.findByText('falhou')).toBeInTheDocument();
  });

  it('criar novo endereço chama createAddress e recarrega', async () => {
    listMock.mockResolvedValueOnce([]);
    createMock.mockResolvedValue(addr('new'));
    listMock.mockResolvedValueOnce([addr('new')]);
    const user = userEvent.setup();
    setup();
    await screen.findByText('Nenhum endereço cadastrado');
    const addBtns = screen.getAllByRole('button', { name: /adicionar endereço/i });
    await user.click(addBtns[addBtns.length - 1]);
    await user.click(screen.getByRole('button', { name: 'form-submit' }));
    await waitFor(() => expect(createMock).toHaveBeenCalledWith('c1', expect.objectContaining({ street: 'R' })));
  });

  it('editar endereço chama updateAddress', async () => {
    listMock.mockResolvedValue([addr('a1')]);
    updateMock.mockResolvedValue(addr('a1'));
    const user = userEvent.setup();
    setup();
    await screen.findByText(/R A,/);
    await user.click(screen.getByRole('button', { name: 'editar' }));
    await user.click(screen.getByRole('button', { name: 'form-submit' }));
    await waitFor(() => expect(updateMock).toHaveBeenCalledWith('c1', 'a1', expect.objectContaining({ street: 'R' })));
  });

  it('excluir endereço confirma e chama deleteAddress', async () => {
    listMock.mockResolvedValue([addr('a1')]);
    deleteMock.mockResolvedValue(undefined);
    const user = userEvent.setup();
    setup();
    await screen.findByText(/R A,/);
    await user.click(screen.getByRole('button', { name: 'excluir' }));
    await user.click(screen.getByRole('button', { name: 'Excluir' }));
    await waitFor(() => expect(deleteMock).toHaveBeenCalledWith('c1', 'a1'));
  });

  it('excluir cancelado fecha diálogo sem chamar API', async () => {
    listMock.mockResolvedValue([addr('a1')]);
    const user = userEvent.setup();
    setup();
    await screen.findByText(/R A,/);
    await user.click(screen.getByRole('button', { name: 'excluir' }));
    await user.click(screen.getByRole('button', { name: 'Cancelar' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(deleteMock).not.toHaveBeenCalled();
  });

  it('erro ao criar notifica', async () => {
    listMock.mockResolvedValue([]);
    createMock.mockRejectedValue(new Error('500'));
    const user = userEvent.setup();
    setup();
    await screen.findByText('Nenhum endereço cadastrado');
    const addBtns = screen.getAllByRole('button', { name: /adicionar endereço/i });
    await user.click(addBtns[addBtns.length - 1]);
    await user.click(screen.getByRole('button', { name: 'form-submit' }));
    expect(await screen.findByText('500')).toBeInTheDocument();
  });
});
