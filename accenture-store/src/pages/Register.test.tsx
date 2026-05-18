import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../theme/theme';
import Register from './Register';
import { SnackbarProvider } from '../contexts/SnackbarContext';

vi.mock('../api/auth', () => ({ register: vi.fn() }));
import { register as registerApi } from '../api/auth';
const apiMock = registerApi as unknown as ReturnType<typeof vi.fn>;

function LocationProbe() {
  const l = useLocation();
  return <div data-testid="loc">{l.pathname}</div>;
}

function setup() {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter initialEntries={['/register']}>
        <SnackbarProvider>
          <Routes>
            <Route path="/register" element={<Register />} />
            <Route path="*" element={<LocationProbe />} />
          </Routes>
        </SnackbarProvider>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

async function fillValid(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText(/nome completo/i), 'João Silva');
  await user.type(screen.getByLabelText(/cpf/i), '12345678901');
  await user.type(screen.getByLabelText(/telefone/i), '11987654321');
  await user.type(screen.getByLabelText(/^e-mail/i), 'a@b.c');
  await user.type(screen.getByLabelText(/^senha/i), 'Strong#Pass1');
  await user.type(screen.getByLabelText(/confirmar senha/i), 'Strong#Pass1');
}

describe('Register', () => {
  beforeEach(() => apiMock.mockReset());

  it('formata CPF enquanto digita', async () => {
    const user = userEvent.setup();
    setup();
    const cpf = screen.getByLabelText(/cpf/i) as HTMLInputElement;
    await user.type(cpf, '12345678901');
    expect(cpf.value).toBe('123.456.789-01');
  });

  it('formata telefone (celular 11 dígitos)', async () => {
    const user = userEvent.setup();
    setup();
    const tel = screen.getByLabelText(/telefone/i) as HTMLInputElement;
    await user.type(tel, '11987654321');
    expect(tel.value).toBe('(11) 98765-4321');
  });

  it('formata telefone fixo (10 dígitos)', async () => {
    const user = userEvent.setup();
    setup();
    const tel = screen.getByLabelText(/telefone/i) as HTMLInputElement;
    await user.type(tel, '1133334444');
    expect(tel.value).toBe('(11) 3333-4444');
  });

  it('mostra força da senha (fraca/forte)', async () => {
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/^senha/i), 'abc');
    expect(await screen.findByText(/Força da senha: Fraca/i)).toBeInTheDocument();
    await user.clear(screen.getByLabelText(/^senha/i));
    await user.type(screen.getByLabelText(/^senha/i), 'Strong#Pass1');
    expect(await screen.findByText(/Força da senha: Forte/i)).toBeInTheDocument();
  });

  it('botão fica desabilitado enquanto o formulário é inválido', async () => {
    setup();
    expect(screen.getByRole('button', { name: /criar conta/i })).toBeDisabled();
  });

  it('valida nome muito curto após perder o foco', async () => {
    const user = userEvent.setup();
    setup();
    const nome = screen.getByLabelText(/nome completo/i);
    await user.type(nome, 'A');
    await user.tab();
    expect(await screen.findByText('Nome muito curto')).toBeInTheDocument();
  });

  it('valida CPF incompleto após perder o foco', async () => {
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/cpf/i), '123');
    await user.tab();
    expect(await screen.findByText('CPF deve ter 11 dígitos')).toBeInTheDocument();
  });

  it('valida telefone curto após perder o foco', async () => {
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/telefone/i), '11');
    await user.tab();
    expect(await screen.findByText('Telefone deve ter pelo menos 10 dígitos')).toBeInTheDocument();
  });

  it('valida email inválido após perder o foco', async () => {
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/^e-mail/i), 'inv');
    await user.tab();
    expect(await screen.findByText('E-mail inválido')).toBeInTheDocument();
  });

  it('valida senha curta após perder o foco', async () => {
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/^senha/i), '123');
    await user.tab();
    expect(await screen.findByText('Senha precisa ter pelo menos 8 caracteres')).toBeInTheDocument();
  });

  it('valida divergência de confirmação após perder o foco', async () => {
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/^senha/i), 'Strong#Pass1');
    await user.type(screen.getByLabelText(/confirmar senha/i), 'outra1234');
    await user.tab();
    expect(await screen.findByText('Senhas não conferem')).toBeInTheDocument();
  });

  it('telefone não aceita letras (apenas dígitos com máscara)', async () => {
    const user = userEvent.setup();
    setup();
    const tel = screen.getByLabelText(/telefone/i) as HTMLInputElement;
    await user.type(tel, 'abc11987654321xyz');
    expect(tel.value).toBe('(11) 98765-4321');
  });

  it('submit válido chama API e navega para login', async () => {
    apiMock.mockResolvedValue({});
    const user = userEvent.setup();
    setup();
    await fillValid(user);
    await user.click(screen.getByRole('button', { name: /criar conta/i }));
    await waitFor(() => expect(apiMock).toHaveBeenCalled());
    expect(apiMock.mock.calls[0][0]).toMatchObject({
      email: 'a@b.c', name: 'João Silva', cpf: '12345678901', phone: '11987654321',
    });
    await waitFor(() => expect(screen.getByTestId('loc')).toHaveTextContent('/login'));
  });

  it('exibe erro do servidor', async () => {
    apiMock.mockRejectedValue(new Error('email já existe'));
    const user = userEvent.setup();
    setup();
    await fillValid(user);
    await user.click(screen.getByRole('button', { name: /criar conta/i }));
    expect(await screen.findByText('email já existe')).toBeInTheDocument();
  });
});
