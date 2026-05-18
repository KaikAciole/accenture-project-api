import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import PrivateRoute from './PrivateRoute';

const useAuthMock = vi.fn();
vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => useAuthMock(),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

function setup(path = '/protected') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/login" element={<div>login page</div>} />
        <Route path="/admin" element={<div>admin home</div>} />
        <Route
          path="/protected"
          element={
            <PrivateRoute>
              <div>conteudo privado</div>
            </PrivateRoute>
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe('PrivateRoute', () => {
  beforeEach(() => useAuthMock.mockReset());

  it('não autenticado: redireciona para /login', () => {
    useAuthMock.mockReturnValue({ isAuthenticated: false, isAdmin: false });
    setup();
    expect(screen.getByText('login page')).toBeInTheDocument();
  });

  it('autenticado e não admin: renderiza filho', () => {
    useAuthMock.mockReturnValue({ isAuthenticated: true, isAdmin: false });
    setup();
    expect(screen.getByText('conteudo privado')).toBeInTheDocument();
  });

  it('admin: redireciona para /admin mesmo autenticado', () => {
    useAuthMock.mockReturnValue({ isAuthenticated: true, isAdmin: true });
    setup();
    expect(screen.getByText('admin home')).toBeInTheDocument();
  });
});
