import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../theme/theme';
import ResetPassword from './ResetPassword';
import { SnackbarProvider } from '../contexts/SnackbarContext';
import { ApiError } from '../api/client';

vi.mock('../api/auth', () => ({ resetPassword: vi.fn() }));
import { resetPassword as resetPasswordApi } from '../api/auth';
const apiMock = resetPasswordApi as unknown as ReturnType<typeof vi.fn>;

function LocationProbe() {
  const l = useLocation();
  return <div data-testid="loc">{l.pathname}</div>;
}

function setup(qs = '?email=a%40b.c&token=tk') {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter initialEntries={[`/reset-password${qs}`]}>
        <SnackbarProvider>
          <Routes>
            <Route path="/reset-password" element={<ResetPassword />} />
            <Route path="*" element={<LocationProbe />} />
          </Routes>
        </SnackbarProvider>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe('ResetPassword', () => {
  beforeEach(() => apiMock.mockReset());

  it('mostra erro quando link inválido (sem email/token)', () => {
    setup('');
    expect(screen.getByText(/Link inválido/i)).toBeInTheDocument();
  });

  it('valida senha curta', async () => {
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/nova senha/i), '123');
    await user.type(screen.getByLabelText(/confirmar senha/i), '123');
    await user.click(screen.getByRole('button', { name: /redefinir senha/i }));
    expect(await screen.findByText('Senha precisa ter pelo menos 8 caracteres')).toBeInTheDocument();
  });

  it('valida divergência de senhas', async () => {
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/nova senha/i), '12345678');
    await user.type(screen.getByLabelText(/confirmar senha/i), '99999999');
    await user.click(screen.getByRole('button', { name: /redefinir senha/i }));
    expect(await screen.findByText('Senhas não conferem')).toBeInTheDocument();
  });

  it('sucesso navega para /login', async () => {
    apiMock.mockResolvedValue(undefined);
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/nova senha/i), '12345678');
    await user.type(screen.getByLabelText(/confirmar senha/i), '12345678');
    await user.click(screen.getByRole('button', { name: /redefinir senha/i }));
    await waitFor(() => expect(screen.getByTestId('loc')).toHaveTextContent('/login'));
  });

  it('422 mostra mensagem de token inválido', async () => {
    apiMock.mockRejectedValue(new ApiError('expired', 422, null));
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/nova senha/i), '12345678');
    await user.type(screen.getByLabelText(/confirmar senha/i), '12345678');
    await user.click(screen.getByRole('button', { name: /redefinir senha/i }));
    expect(await screen.findByText(/Token inválido/i)).toBeInTheDocument();
  });

  it('erro genérico mostra message', async () => {
    apiMock.mockRejectedValue(new Error('boom'));
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/nova senha/i), '12345678');
    await user.type(screen.getByLabelText(/confirmar senha/i), '12345678');
    await user.click(screen.getByRole('button', { name: /redefinir senha/i }));
    expect(await screen.findByText('boom')).toBeInTheDocument();
  });

  it('alterna visibilidade da senha', async () => {
    const user = userEvent.setup();
    setup();
    const pwd = screen.getByLabelText(/nova senha/i) as HTMLInputElement;
    expect(pwd.type).toBe('password');
    await user.click(screen.getByRole('button', { name: /alternar senha/i }));
    expect(pwd.type).toBe('text');
  });
});
