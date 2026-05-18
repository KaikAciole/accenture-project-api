import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../theme/theme';
import { SnackbarProvider } from '../contexts/SnackbarContext';
import { AuthProvider } from '../contexts/AuthContext';
import { CartProvider } from '../contexts/CartContext';
import Home from './Home';

vi.mock('../api/products', () => ({ listProducts: vi.fn() }));
import { listProducts } from '../api/products';
const listProductsMock = listProducts as unknown as ReturnType<typeof vi.fn>;

function renderHome(route = '/') {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter initialEntries={[route]}>
        <SnackbarProvider>
          <AuthProvider>
            <CartProvider>
              <Home />
            </CartProvider>
          </AuthProvider>
        </SnackbarProvider>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

const product = (id: string, name: string) => ({ id, sku: id, name, price: 10, stockQuantity: 5 });

describe('Home', () => {
  beforeEach(() => listProductsMock.mockReset());

  it('lista produtos retornados como array', async () => {
    listProductsMock.mockResolvedValue([product('1', 'A'), product('2', 'B')]);
    renderHome();
    expect(await screen.findByText('A')).toBeInTheDocument();
    expect(screen.getByText('B')).toBeInTheDocument();
  });

  it('lista produtos retornados como Paginated.content', async () => {
    listProductsMock.mockResolvedValue({ content: [product('1', 'X')], totalPages: 3 });
    renderHome();
    expect(await screen.findByText('X')).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: /pagination/i })).toBeInTheDocument();
  });

  it('aceita Paginated.items', async () => {
    listProductsMock.mockResolvedValue({ items: [product('1', 'I')], totalPages: 1 });
    renderHome();
    expect(await screen.findByText('I')).toBeInTheDocument();
  });

  it('mensagem quando vazio', async () => {
    listProductsMock.mockResolvedValue([]);
    renderHome();
    expect(await screen.findByText('Nenhum produto encontrado.')).toBeInTheDocument();
  });

  it('mostra erro quando API falha', async () => {
    listProductsMock.mockRejectedValue(new Error('rede caiu'));
    renderHome();
    expect(await screen.findByText(/Não foi possível carregar os produtos: rede caiu/)).toBeInTheDocument();
  });

  it('mostra título com query quando ?q= presente', async () => {
    listProductsMock.mockResolvedValue([]);
    renderHome('/?q=teclado');
    expect(await screen.findByText(/Resultados para “teclado”/)).toBeInTheDocument();
    expect(listProductsMock).toHaveBeenCalledWith({ page: 0, size: 12, q: 'teclado' });
  });

  it('mudar de página chama API com novo page', async () => {
    listProductsMock.mockResolvedValue({ content: [product('1', 'A')], totalPages: 3 });
    const user = userEvent.setup();
    renderHome();
    await screen.findByText('A');
    await user.click(screen.getByRole('button', { name: 'Go to page 2' }));
    await waitFor(() => expect(listProductsMock).toHaveBeenLastCalledWith({ page: 1, size: 12, q: '' }));
  });
});
