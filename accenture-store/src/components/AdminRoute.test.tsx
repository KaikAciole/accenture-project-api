import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import AdminRoute from './AdminRoute';

const useAuthMock = vi.fn();
const notifyMock = vi.fn();

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => useAuthMock(),
}));
vi.mock('../contexts/SnackbarContext', () => ({
  useSnackbar: () => ({ notify: notifyMock }),
}));

function setup() {
  return render(
    <MemoryRouter initialEntries={['/admin-only']}>
      <Routes>
        <Route path="/login" element={<div>login page</div>} />
        <Route path="/" element={<div>home page</div>} />
        <Route
          path="/admin-only"
          element={
            <AdminRoute>
              <div>painel admin</div>
            </AdminRoute>
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe('AdminRoute', () => {
  beforeEach(() => {
    useAuthMock.mockReset();
    notifyMock.mockReset();
  });

  it('não autenticado: vai pra /login sem notificar', () => {
    useAuthMock.mockReturnValue({ isAuthenticated: false, isAdmin: false });
    setup();
    expect(screen.getByText('login page')).toBeInTheDocument();
    expect(notifyMock).not.toHaveBeenCalled();
  });

  it('autenticado mas sem ADMIN: redireciona pra / e mostra erro no snackbar', async () => {
    useAuthMock.mockReturnValue({ isAuthenticated: true, isAdmin: false });
    setup();
    expect(screen.getByText('home page')).toBeInTheDocument();
    await waitFor(() =>
      expect(notifyMock).toHaveBeenCalledWith('Você não tem permissão pra essa área', 'error'),
    );
  });

  it('autenticado e admin: renderiza filho', () => {
    useAuthMock.mockReturnValue({ isAuthenticated: true, isAdmin: true });
    setup();
    expect(screen.getByText('painel admin')).toBeInTheDocument();
  });
});
