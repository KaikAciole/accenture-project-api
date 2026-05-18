import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import App from './App';

vi.mock('./api/products', () => ({ listProducts: vi.fn().mockResolvedValue([]) }));
vi.mock('./api/orders', () => ({
  listMyOrders: vi.fn().mockResolvedValue([]),
  listAllOrders: vi.fn().mockResolvedValue([]),
  createOrder: vi.fn(),
  getOrder: vi.fn(),
  cancelOrder: vi.fn(),
}));
vi.mock('./api/payments', () => ({
  createPayment: vi.fn(), processPayment: vi.fn(),
  getPayment: vi.fn(), getPaymentByOrder: vi.fn(),
}));
vi.mock('./api/wallets', () => ({
  COMPANY_OWNER_ID: 'comp',
  getWalletByOwner: vi.fn().mockResolvedValue({ balance: 0 }),
  listWalletTransactions: vi.fn().mockResolvedValue([]),
  listTransactionsByOwner: vi.fn().mockResolvedValue([]),
  listCompanyTransactions: vi.fn().mockResolvedValue([]),
  createTopUp: vi.fn(), submitTopUp: vi.fn(),
}));
vi.mock('./api/customers', () => ({
  getCustomer: vi.fn().mockResolvedValue({ name: 'X' }),
  updateCustomer: vi.fn(), listCustomers: vi.fn().mockResolvedValue([]),
  deleteCustomer: vi.fn(), listAddresses: vi.fn().mockResolvedValue([]),
  getAddress: vi.fn(), createAddress: vi.fn(),
  updateAddress: vi.fn(), deleteAddress: vi.fn(),
}));
vi.mock('./api/auth', () => ({
  login: vi.fn(), register: vi.fn(),
  forgotPassword: vi.fn(), resetPassword: vi.fn(),
}));
vi.mock('./api/assistant', () => ({ askAssistantStream: vi.fn() }));
vi.mock('./api/cep', () => ({ lookupCep: vi.fn() }));

function renderApp(route = '/') {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <App />
    </MemoryRouter>,
  );
}

describe('App', () => {
  beforeEach(() => {
    localStorage.clear();
    document.cookie.split(';').forEach((c) => {
      const name = c.split('=')[0].trim();
      if (name) document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
    });
  });

  it('renderiza home com MinimalHeader quando não autenticado', async () => {
    renderApp('/');
    expect(await screen.findByRole('link', { name: /entrar/i })).toBeInTheDocument();
  });

  it('mostra a página de login sem chrome', async () => {
    renderApp('/login');
    await waitFor(() => expect(screen.getByText('Bem-vindo de volta')).toBeInTheDocument());
    expect(screen.queryByRole('link', { name: /entrar/i })).not.toBeInTheDocument();
  });

  it('mostra a página de registro', async () => {
    renderApp('/register');
    await waitFor(() => expect(screen.getByText('Crie sua conta')).toBeInTheDocument());
  });

  it('rota privada redireciona não autenticado para /login', async () => {
    renderApp('/cart');
    await waitFor(() => expect(screen.getByText('Bem-vindo de volta')).toBeInTheDocument());
  });
});
