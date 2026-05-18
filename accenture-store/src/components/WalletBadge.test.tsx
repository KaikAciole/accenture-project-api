import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../theme/theme';
import WalletBadge from './WalletBadge';

const useAuthMock = vi.fn();
const getWalletByOwnerMock = vi.fn();

vi.mock('../contexts/AuthContext', () => ({ useAuth: () => useAuthMock() }));
vi.mock('../api/wallets', () => ({ getWalletByOwner: (...args: unknown[]) => getWalletByOwnerMock(...args) }));

function renderBadge() {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter>
        <WalletBadge />
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe('WalletBadge', () => {
  beforeEach(() => {
    useAuthMock.mockReset();
    getWalletByOwnerMock.mockReset();
    vi.useRealTimers();
  });

  it('não renderiza quando não autenticado', () => {
    useAuthMock.mockReturnValue({ user: null, isAuthenticated: false, isAdmin: false });
    const { container } = renderBadge();
    expect(container).toBeEmptyDOMElement();
  });

  it('não renderiza para admin', () => {
    useAuthMock.mockReturnValue({ user: { customerId: 'c' }, isAuthenticated: true, isAdmin: true });
    const { container } = renderBadge();
    expect(container).toBeEmptyDOMElement();
  });

  it('busca saldo e exibe formatado em BRL', async () => {
    useAuthMock.mockReturnValue({ user: { customerId: 'c1' }, isAuthenticated: true, isAdmin: false });
    getWalletByOwnerMock.mockResolvedValue({ balance: 123.45 });

    renderBadge();

    expect(await screen.findByText(/R\$\s?123,45/)).toBeInTheDocument();
    expect(getWalletByOwnerMock).toHaveBeenCalledWith('c1', 'CUSTOMER');
  });

  it('usa 0 quando wallet.balance vem undefined', async () => {
    useAuthMock.mockReturnValue({ user: { customerId: 'c1' }, isAuthenticated: true, isAdmin: false });
    getWalletByOwnerMock.mockResolvedValue({});
    renderBadge();
    expect(await screen.findByText(/R\$\s?0,00/)).toBeInTheDocument();
  });

  it('em erro: não renderiza nada (balance null)', async () => {
    useAuthMock.mockReturnValue({ user: { customerId: 'c1' }, isAuthenticated: true, isAdmin: false });
    getWalletByOwnerMock.mockRejectedValue(new Error('boom'));
    const { container } = renderBadge();
    await waitFor(() => expect(container).toBeEmptyDOMElement());
  });

  it('quando não há customerId mostra nada', async () => {
    useAuthMock.mockReturnValue({ user: {}, isAuthenticated: true, isAdmin: false });
    const { container } = renderBadge();
    await waitFor(() => expect(container).toBeEmptyDOMElement());
  });
});
