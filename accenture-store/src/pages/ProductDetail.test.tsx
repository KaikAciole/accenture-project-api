import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../theme/theme';
import { SnackbarProvider } from '../contexts/SnackbarContext';
import ProductDetail from './ProductDetail';

vi.mock('../api/products', () => ({ getProduct: vi.fn() }));
const addItemMock = vi.fn();
const notifyMock = vi.fn();
vi.mock('../contexts/CartContext', () => ({ useCart: () => ({ addItem: addItemMock }) }));
vi.mock('../contexts/SnackbarContext', async () => {
  const actual = await vi.importActual<typeof import('../contexts/SnackbarContext')>(
    '../contexts/SnackbarContext',
  );
  return { ...actual, useSnackbar: () => ({ notify: notifyMock }) };
});

import { getProduct } from '../api/products';
const getProductMock = getProduct as unknown as ReturnType<typeof vi.fn>;

function LocationProbe() {
  const l = useLocation();
  return <div data-testid="loc">{l.pathname}</div>;
}

function setup(id = 'p1') {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter initialEntries={[`/products/${id}`]}>
        <SnackbarProvider>
          <Routes>
            <Route path="/products/:id" element={<ProductDetail />} />
            <Route path="*" element={<LocationProbe />} />
          </Routes>
        </SnackbarProvider>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

const baseProduct = {
  id: 'p1', sku: 'SKU1', name: 'Notebook', price: 5000, stockQuantity: 10,
  description: 'descrição', category: 'Eletrônicos', imageUrl: 'http://x/i.png',
};

describe('ProductDetail', () => {
  beforeEach(() => {
    getProductMock.mockReset();
    addItemMock.mockReset();
    notifyMock.mockReset();
  });

  it('mostra skeleton no loading', () => {
    getProductMock.mockImplementation(() => new Promise(() => {}));
    setup();
    expect(screen.queryByText('Notebook')).not.toBeInTheDocument();
  });

  it('exibe erro quando getProduct falha', async () => {
    getProductMock.mockRejectedValue(new Error('not found'));
    setup();
    expect(await screen.findByText('not found')).toBeInTheDocument();
  });

  it('renderiza produto completo e breadcrumb com categoria', async () => {
    getProductMock.mockResolvedValue(baseProduct);
    setup();
    await screen.findByRole('heading', { name: 'Notebook' });
    expect(screen.getByText('descrição')).toBeInTheDocument();
    expect(screen.getAllByText('Eletrônicos').length).toBeGreaterThan(0);
    expect(screen.getByText('SKU: SKU1')).toBeInTheDocument();
  });

  it('adiciona ao carrinho com quantidade', async () => {
    getProductMock.mockResolvedValue(baseProduct);
    const user = userEvent.setup();
    setup();
    await screen.findByRole('heading', { name: 'Notebook' });
    await user.click(screen.getByRole('button', { name: /aumentar/i }));
    await user.click(screen.getByRole('button', { name: /aumentar/i }));
    await user.click(screen.getByRole('button', { name: /adicionar ao carrinho/i }));
    expect(addItemMock).toHaveBeenCalledWith(expect.objectContaining({ id: 'p1' }), 3);
    expect(notifyMock).toHaveBeenCalledWith('3× adicionados ao carrinho', 'success');
  });

  it('diminuir quantidade respeita mínimo de 1', async () => {
    getProductMock.mockResolvedValue(baseProduct);
    const user = userEvent.setup();
    setup();
    await screen.findByRole('heading', { name: 'Notebook' });
    expect(screen.getByRole('button', { name: /diminuir/i })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: /aumentar/i }));
    expect(screen.getByRole('button', { name: /diminuir/i })).not.toBeDisabled();
  });

  it('quantidade não excede o estoque máximo', async () => {
    getProductMock.mockResolvedValue({ ...baseProduct, stockQuantity: 2 });
    const user = userEvent.setup();
    setup();
    await screen.findByRole('heading', { name: 'Notebook' });
    await user.click(screen.getByRole('button', { name: /aumentar/i }));
    expect(screen.getByRole('button', { name: /aumentar/i })).toBeDisabled();
  });

  it('comprar agora adiciona e vai para /cart', async () => {
    getProductMock.mockResolvedValue(baseProduct);
    const user = userEvent.setup();
    setup();
    await screen.findByRole('heading', { name: 'Notebook' });
    await user.click(screen.getByRole('button', { name: /comprar agora/i }));
    expect(addItemMock).toHaveBeenCalled();
    await waitFor(() => expect(screen.getByTestId('loc')).toHaveTextContent('/cart'));
  });

  it('produto esgotado desabilita ações', async () => {
    getProductMock.mockResolvedValue({ ...baseProduct, stockQuantity: 0 });
    setup();
    await screen.findByRole('heading', { name: 'Notebook' });
    expect(screen.getByRole('button', { name: /adicionar ao carrinho/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /comprar agora/i })).toBeDisabled();
    expect(screen.getByText('Indisponível')).toBeInTheDocument();
  });

  it('estoque baixo mostra chip "Restam N"', async () => {
    getProductMock.mockResolvedValue({ ...baseProduct, stockQuantity: 3 });
    setup();
    await screen.findByRole('heading', { name: 'Notebook' });
    expect(screen.getByText(/Restam 3 unidades/)).toBeInTheDocument();
  });

  it('produto sem imagem mostra ícone fallback', async () => {
    getProductMock.mockResolvedValue({ ...baseProduct, imageUrl: undefined });
    setup();
    await screen.findByRole('heading', { name: 'Notebook' });
    expect(screen.queryByAltText('Notebook')).not.toBeInTheDocument();
  });
});
