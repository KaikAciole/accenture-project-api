import { describe, it, expect } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen } from '../test/utils';
import ProductCard from './ProductCard';
import type { Product } from '../api/types';

const baseProduct: Product = {
  id: 'p-1',
  sku: 'SKU-1',
  name: 'Camiseta Accenture',
  price: 129.9,
  stockQuantity: 20,
  imageUrl: 'https://cdn.example.com/img.png',
};

describe('ProductCard', () => {
  it('renderiza nome, preço formatado em BRL e imagem', () => {
    renderWithProviders(<ProductCard product={baseProduct} />);

    expect(screen.getByText('Camiseta Accenture')).toBeInTheDocument();
    expect(screen.getByText(/R\$\s?129,90/)).toBeInTheDocument();

    const img = screen.getByAltText('Camiseta Accenture') as HTMLImageElement;
    expect(img.src).toContain('img.png');
  });

  it('mostra chip "Esgotado" e desabilita botão quando stockQuantity é 0', () => {
    renderWithProviders(<ProductCard product={{ ...baseProduct, stockQuantity: 0 }} />);

    expect(screen.getByText('Esgotado')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /indisponível/i })).toBeDisabled();
  });

  it('mostra chip "Restam N" quando estoque é baixo (1..5)', () => {
    renderWithProviders(<ProductCard product={{ ...baseProduct, stockQuantity: 3 }} />);
    expect(screen.getByText('Restam 3')).toBeInTheDocument();
  });

  it('exibe ícone de imagem ausente quando imageUrl não vem', () => {
    const { container } = renderWithProviders(
      <ProductCard product={{ ...baseProduct, imageUrl: undefined }} />,
    );
    expect(screen.queryByAltText('Camiseta Accenture')).not.toBeInTheDocument();
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('o link do card aponta para /products/:id', () => {
    renderWithProviders(<ProductCard product={baseProduct} />);
    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', '/products/p-1');
  });

  it('usa sku como fallback no link quando id está ausente', () => {
    renderWithProviders(<ProductCard product={{ ...baseProduct, id: undefined }} />);
    expect(screen.getByRole('link')).toHaveAttribute('href', '/products/SKU-1');
  });

  it('clicar em "Adicionar ao carrinho" dispara o feedback de sucesso', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ProductCard product={baseProduct} />);

    await user.click(screen.getByRole('button', { name: /adicionar ao carrinho/i }));

    expect(await screen.findByText('Produto adicionado ao carrinho')).toBeInTheDocument();
  });
});
