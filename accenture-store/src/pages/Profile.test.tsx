import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../theme/theme';
import { SnackbarProvider } from '../contexts/SnackbarContext';
import Profile from './Profile';

const useAuthMock = vi.fn();
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => useAuthMock() }));
vi.mock('../api/customers', () => ({
  getCustomer: vi.fn(),
  updateCustomer: vi.fn(),
}));

import { getCustomer, updateCustomer } from '../api/customers';
const getCustomerMock = getCustomer as unknown as ReturnType<typeof vi.fn>;
const updateCustomerMock = updateCustomer as unknown as ReturnType<typeof vi.fn>;

function setup() {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter>
        <SnackbarProvider>
          <Profile />
        </SnackbarProvider>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe('Profile', () => {
  beforeEach(() => {
    useAuthMock.mockReset();
    getCustomerMock.mockReset();
    updateCustomerMock.mockReset();
    useAuthMock.mockReturnValue({ user: { customerId: 'c1' } });
  });

  it('skeleton enquanto carrega', () => {
    getCustomerMock.mockImplementation(() => new Promise(() => {}));
    setup();
    expect(screen.queryByLabelText('Nome')).not.toBeInTheDocument();
  });

  it('exibe erro quando getCustomer falha', async () => {
    getCustomerMock.mockRejectedValue(new Error('falhou'));
    setup();
    expect(await screen.findByText('falhou')).toBeInTheDocument();
  });

  it('renderiza dados e salva alterações com sucesso', async () => {
    getCustomerMock.mockResolvedValue({ name: 'João', email: 'j@b.c', cpf: '123', phone: '11999998888' });
    updateCustomerMock.mockResolvedValue({});
    const user = userEvent.setup();
    setup();

    const nome = await screen.findByLabelText('Nome') as HTMLInputElement;
    expect(nome.value).toBe('João');
    expect((screen.getByLabelText('CPF') as HTMLInputElement).disabled).toBe(true);
    expect((screen.getByLabelText('Telefone') as HTMLInputElement).value).toBe('(11) 99999-8888');

    await user.clear(nome);
    await user.type(nome, 'Maria');
    await user.click(screen.getByRole('button', { name: /salvar alterações/i }));

    await waitFor(() => expect(updateCustomerMock).toHaveBeenCalledWith('c1', {
      name: 'Maria', email: 'j@b.c', phone: '11999998888',
    }));
    expect(await screen.findByText('Perfil atualizado')).toBeInTheDocument();
  });

  it('mudança de email mostra notify específico', async () => {
    getCustomerMock.mockResolvedValue({ name: 'Xavier', email: 'old@x.c', phone: '11999998888' });
    updateCustomerMock.mockResolvedValue({});
    const user = userEvent.setup();
    setup();

    const email = await screen.findByLabelText('E-mail') as HTMLInputElement;
    await user.clear(email);
    await user.type(email, 'new@x.c');
    await user.click(screen.getByRole('button', { name: /salvar alterações/i }));
    expect(await screen.findByText(/Email atualizado\. Use new@x\.c/)).toBeInTheDocument();
  });

  it('falha no save mostra notify de erro', async () => {
    getCustomerMock.mockResolvedValue({ name: 'Xavier', email: 'a@b.c', phone: '11999998888' });
    updateCustomerMock.mockRejectedValue(new Error('500'));
    const user = userEvent.setup();
    setup();
    await screen.findByLabelText('Nome');
    await user.click(screen.getByRole('button', { name: /salvar alterações/i }));
    expect(await screen.findByText('500')).toBeInTheDocument();
  });

  it('botão Salvar fica desabilitado com telefone inválido', async () => {
    getCustomerMock.mockResolvedValue({ name: 'X', email: 'a@b.c', phone: '11' });
    setup();
    await screen.findByLabelText('Nome');
    expect(screen.getByRole('button', { name: /salvar alterações/i })).toBeDisabled();
  });

  it('telefone bloqueia letras digitadas', async () => {
    getCustomerMock.mockResolvedValue({ name: 'X', email: 'a@b.c', phone: '' });
    const user = userEvent.setup();
    setup();
    const tel = await screen.findByLabelText('Telefone') as HTMLInputElement;
    await user.type(tel, 'abc11987654321');
    expect(tel.value).toBe('(11) 98765-4321');
  });

  it('sem customerId não chama API', () => {
    useAuthMock.mockReturnValue({ user: null });
    setup();
    expect(getCustomerMock).not.toHaveBeenCalled();
  });
});
