import { describe, it, expect } from 'vitest';
import userEvent from '@testing-library/user-event';
import { Box } from '@mui/material';
import ProductCard from '../components/ProductCard';
import { useCart } from '../contexts/CartContext';
import type { Product } from '../api/types';
import { renderWithProviders, screen } from './utils';

const camisa: Product = { id: 'p-1', sku: 'SKU-1', name: 'Camisa', price: 100, stockQuantity: 10 };
const caneca: Product = { id: 'p-2', sku: 'SKU-2', name: 'Caneca', price: 25, stockQuantity: 10 };

function CartSummary() {
  const { items, totalItems, subtotal } = useCart();
  return (
    <Box>
      <span data-testid="total-items">{totalItems}</span>
      <span data-testid="subtotal">{subtotal}</span>
      <ul>
        {items.map((it) => (
          <li key={it.sku} data-testid={`row-${it.sku}`}>
            {it.name} × {it.quantity}
          </li>
        ))}
      </ul>
    </Box>
  );
}

function Catalog() {
  return (
    <Box>
      <ProductCard product={camisa} />
      <ProductCard product={caneca} />
      <CartSummary />
    </Box>
  );
}

describe('integração: adicionar produtos ao carrinho', () => {
  it('adiciona dois produtos diferentes e atualiza totais + snackbar', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Catalog />);

    expect(screen.getByTestId('total-items')).toHaveTextContent('0');
    expect(screen.getByTestId('subtotal')).toHaveTextContent('0');

    const [btnCamisa, btnCaneca] = screen.getAllByRole('button', { name: /adicionar ao carrinho/i });

    await user.click(btnCamisa);
    expect(await screen.findByText('Produto adicionado ao carrinho')).toBeInTheDocument();

    await user.click(btnCaneca);

    expect(screen.getByTestId('total-items')).toHaveTextContent('2');
    expect(screen.getByTestId('subtotal')).toHaveTextContent('125');
    expect(screen.getByTestId('row-SKU-1')).toHaveTextContent('Camisa × 1');
    expect(screen.getByTestId('row-SKU-2')).toHaveTextContent('Caneca × 1');
  });

  it('clicar duas vezes no mesmo produto soma a quantidade', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Catalog />);
    const [btnCamisa] = screen.getAllByRole('button', { name: /adicionar ao carrinho/i });

    await user.click(btnCamisa);
    await user.click(btnCamisa);

    expect(screen.getByTestId('total-items')).toHaveTextContent('2');
    expect(screen.getByTestId('subtotal')).toHaveTextContent('200');
    expect(screen.getByTestId('row-SKU-1')).toHaveTextContent('Camisa × 2');
  });
});
