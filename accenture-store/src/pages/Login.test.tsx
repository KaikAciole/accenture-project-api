import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../theme/theme';
import Login from './Login';
import { ApiError } from '../api/client';

vi.mock('../api/auth', () => ({ login: vi.fn() }));
const authState: { roles: string[]; authed: boolean } = { roles: [], authed: false };
const loginAuthMock = vi.fn((_token: string, extra?: { roles?: string[] }) => {
  authState.roles = extra?.roles || [];
  authState.authed = true;
});
vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({
    login: loginAuthMock,
    isAuthenticated: authState.authed,
    isAdmin: authState.roles.includes('ADMIN'),
  }),
}));

import { login as apiLogin } from '../api/auth';
const apiLoginMock = apiLogin as unknown as ReturnType<typeof vi.fn>;

function LocationProbe() {
  const l = useLocation();
  return <div data-testid="loc">{l.pathname}</div>;
}

function setup(initial: { pathname: string; state?: unknown }[] = [{ pathname: '/login' }]) {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter initialEntries={initial}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="*" element={<LocationProbe />} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe('Login', () => {
  beforeEach(() => {
    apiLoginMock.mockReset();
    loginAuthMock.mockClear();
    authState.roles = [];
    authState.authed = false;
  });

  it('valida email vazio', async () => {
    const user = userEvent.setup();
    setup();
    await user.click(screen.getByRole('button', { name: /entrar/i }));
    expect(await screen.findByText('Informe seu e-mail')).toBeInTheDocument();
  });

  it('valida formato de email', async () => {
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/e-mail/i), 'inv');
    await user.type(screen.getByLabelText(/^senha/i), '12345678');
    await user.click(screen.getByRole('button', { name: /entrar/i }));
    expect(await screen.findByText('E-mail inválido')).toBeInTheDocument();
  });

  it('valida senha vazia e curta', async () => {
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/e-mail/i), 'a@b.c');
    await user.click(screen.getByRole('button', { name: /entrar/i }));
    expect(await screen.findByText('Informe sua senha')).toBeInTheDocument();
    await user.type(screen.getByLabelText(/^senha/i), '123');
    await user.click(screen.getByRole('button', { name: /entrar/i }));
    expect(await screen.findByText('Senha precisa ter pelo menos 8 caracteres')).toBeInTheDocument();
  });

  it('login bem sucedido navega para from ou /', async () => {
    apiLoginMock.mockResolvedValue({ token: 't', customerId: 'c1', name: 'x', roles: ['USER'] });
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/e-mail/i), 'a@b.c');
    await user.type(screen.getByLabelText(/^senha/i), '12345678');
    await user.click(screen.getByRole('button', { name: /entrar/i }));
    await waitFor(() => expect(loginAuthMock).toHaveBeenCalled());
    expect(screen.getByTestId('loc')).toHaveTextContent('/');
  });

  it('login admin redireciona para /admin mesmo com from regular', async () => {
    apiLoginMock.mockResolvedValue({ accessToken: 'tk', roles: ['ADMIN'] });
    const user = userEvent.setup();
    setup([{ pathname: '/login', state: { from: { pathname: '/orders' } } }]);
    await user.type(screen.getByLabelText(/e-mail/i), 'a@b.c');
    await user.type(screen.getByLabelText(/^senha/i), '12345678');
    await user.click(screen.getByRole('button', { name: /entrar/i }));
    await waitFor(() => expect(screen.getByTestId('loc')).toHaveTextContent('/admin'));
  });

  it('login normal com from=/admin/* cai para /', async () => {
    apiLoginMock.mockResolvedValue({ token: 'x', roles: ['USER'] });
    const user = userEvent.setup();
    setup([{ pathname: '/login', state: { from: { pathname: '/admin/products' } } }]);
    await user.type(screen.getByLabelText(/e-mail/i), 'a@b.c');
    await user.type(screen.getByLabelText(/^senha/i), '12345678');
    await user.click(screen.getByRole('button', { name: /entrar/i }));
    await waitFor(() => expect(screen.getByTestId('loc')).toHaveTextContent('/'));
  });

  it('exibe erro 401 amigável', async () => {
    apiLoginMock.mockRejectedValue(new ApiError('cred inválida', 401, null));
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/e-mail/i), 'a@b.c');
    await user.type(screen.getByLabelText(/^senha/i), '12345678');
    await user.click(screen.getByRole('button', { name: /entrar/i }));
    expect(await screen.findByText('E-mail ou senha inválidos.')).toBeInTheDocument();
  });

  it('exibe erro genérico quando outra exceção é lançada', async () => {
    apiLoginMock.mockRejectedValue(new Error('rede'));
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/e-mail/i), 'a@b.c');
    await user.type(screen.getByLabelText(/^senha/i), '12345678');
    await user.click(screen.getByRole('button', { name: /entrar/i }));
    expect(await screen.findByText('rede')).toBeInTheDocument();
  });

  it('erro quando servidor não retorna token', async () => {
    apiLoginMock.mockResolvedValue({});
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/e-mail/i), 'a@b.c');
    await user.type(screen.getByLabelText(/^senha/i), '12345678');
    await user.click(screen.getByRole('button', { name: /entrar/i }));
    expect(await screen.findByText('Token não retornado pelo servidor')).toBeInTheDocument();
  });

  it('alterna visibilidade da senha', async () => {
    const user = userEvent.setup();
    setup();
    const pwd = screen.getByLabelText(/^senha/i) as HTMLInputElement;
    expect(pwd.type).toBe('password');
    await user.click(screen.getByRole('button', { name: /alternar senha/i }));
    expect(pwd.type).toBe('text');
  });

  it('links de navegação corretos', () => {
    setup();
    expect(screen.getByRole('link', { name: /esqueci minha senha/i })).toHaveAttribute('href', '/forgot-password');
    expect(screen.getByRole('link', { name: /crie sua conta/i })).toHaveAttribute('href', '/register');
    expect(screen.getByRole('link', { name: /voltar para a loja/i })).toHaveAttribute('href', '/');
  });
});
