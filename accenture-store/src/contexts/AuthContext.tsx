import { createContext, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { jwtDecode } from 'jwt-decode';
import { deleteCookie, getCookie, setCookie } from '../lib/cookies';
import { ApiError } from '../api/client';
import { getCustomer } from '../api/customers';

export interface AuthUser {
  customerId?: string;
  email?: string;
  roles: string[];
  name: string;
}

interface AuthState {
  token: string | null;
  user: AuthUser | null;
}

interface LoginExtra {
  customerId?: string;
  email?: string;
  name?: string;
  roles?: string[];
}

interface JwtClaims {
  customerId?: string;
  sub?: string;
  email?: string;
  roles?: string[];
  name?: string;
}

interface AuthContextValue extends AuthState {
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (token: string, extra?: LoginExtra) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function loadInitial(): AuthState {
  const token = getCookie('access_token');
  const userRaw = localStorage.getItem('user');
  if (!token) return { token: null, user: null };
  if (!userRaw) return { token, user: null };
  try {
    return { token, user: JSON.parse(userRaw) as AuthUser };
  } catch {
    return { token, user: null };
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [{ token, user }, setState] = useState<AuthState>(loadInitial);
  const sessionCheckedRef = useRef(false);

  useEffect(() => {
    if (token) setCookie('access_token', token);
    else deleteCookie('access_token');
    if (user) localStorage.setItem('user', JSON.stringify(user));
    else localStorage.removeItem('user');
  }, [token, user]);

  useEffect(() => {
    if (!token) {
      sessionCheckedRef.current = false;
      return;
    }
    if (sessionCheckedRef.current) return;
    const customerId = user?.customerId;
    if (!customerId) return;
    sessionCheckedRef.current = true;
    getCustomer(customerId)
      .then((customer) => {
        const fetched = customer.name?.trim();
        if (fetched) {
          setState((s) => (s.user ? { token: s.token, user: { ...s.user, name: fetched } } : s));
        }
      })
      .catch((err: unknown) => {
        if (err instanceof ApiError && err.status === 401) {
          setState({ token: null, user: null });
        }
      });
  }, [token, user]);

  const login = (token: string, extra: LoginExtra = {}) => {
    let claims: JwtClaims = {};
    try { claims = jwtDecode<JwtClaims>(token); } catch { /* ignore */ }
    const customerId = extra.customerId || claims.customerId || claims.sub;
    const email = extra.email || claims.email;
    const roles = extra.roles || claims.roles || [];
    const name = (extra.name || claims.name || '').trim();
    setState({ token, user: { customerId, email, roles, name } });
  };

  const logout = () => {
    sessionCheckedRef.current = false;
    setState({ token: null, user: null });
  };

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      user,
      isAuthenticated: !!token,
      isAdmin: !!user?.roles?.includes('ADMIN'),
      login,
      logout,
    }),
    [token, user],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = (): AuthContextValue => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};
