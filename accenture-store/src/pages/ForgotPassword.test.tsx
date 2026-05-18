import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../theme/theme';
import ForgotPassword from './ForgotPassword';
import { SnackbarProvider } from '../contexts/SnackbarContext';

vi.mock('../api/auth', () => ({ forgotPassword: vi.fn() }));
import { forgotPassword as forgotPasswordApi } from '../api/auth';
const apiMock = forgotPasswordApi as unknown as ReturnType<typeof vi.fn>;

function LocationProbe() {
  const l = useLocation();
  return <div data-testid="loc">{l.pathname}</div>;
}

function setup() {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter initialEntries={['/forgot-password']}>
        <SnackbarProvider>
          <Routes>
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="*" element={<LocationProbe />} />
          </Routes>
        </SnackbarProvider>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe('ForgotPassword', () => {
  beforeEach(() => apiMock.mockReset());

  it('valida email inválido sem chamar API', async () => {
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/e-mail/i), 'invalido');
    await user.click(screen.getByRole('button', { name: /enviar link/i }));
    expect(await screen.findByText('E-mail inválido')).toBeInTheDocument();
    expect(apiMock).not.toHaveBeenCalled();
  });

  it('submit envia, mostra snackbar e navega para login', async () => {
    apiMock.mockResolvedValue(undefined);
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/e-mail/i), 'a@b.com');
    await user.click(screen.getByRole('button', { name: /enviar link/i }));
    await waitFor(() => expect(apiMock).toHaveBeenCalledWith('a@b.com'));
    expect(await screen.findByText(/Se o email existir/)).toBeInTheDocument();
    await waitFor(() => expect(screen.getByTestId('loc')).toHaveTextContent('/login'));
  });

  it('mesmo com erro silencioso navega e notifica', async () => {
    apiMock.mockRejectedValue(new Error('boom'));
    const user = userEvent.setup();
    setup();
    await user.type(screen.getByLabelText(/e-mail/i), 'a@b.com');
    await user.click(screen.getByRole('button', { name: /enviar link/i }));
    await waitFor(() => expect(screen.getByTestId('loc')).toHaveTextContent('/login'));
  });
});
