import { describe, it, expect } from 'vitest';
import { renderWithProviders, screen } from '../test/utils';
import AdminLayout from './AdminLayout';

describe('AdminLayout', () => {
  it('renderiza os 5 links do drawer e o conteúdo filho', () => {
    renderWithProviders(
      <AdminLayout>
        <div>conteúdo admin</div>
      </AdminLayout>,
    );
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Produtos')).toBeInTheDocument();
    expect(screen.getByText('Pedidos')).toBeInTheDocument();
    expect(screen.getByText('Clientes')).toBeInTheDocument();
    expect(screen.getByText('Carteira da empresa')).toBeInTheDocument();
    expect(screen.getByText('conteúdo admin')).toBeInTheDocument();

    expect(screen.getByRole('link', { name: /dashboard/i })).toHaveAttribute('href', '/admin');
    expect(screen.getByRole('link', { name: /produtos/i })).toHaveAttribute('href', '/admin/products');
    expect(screen.getByRole('link', { name: /pedidos/i })).toHaveAttribute('href', '/admin/orders');
    expect(screen.getByRole('link', { name: /clientes/i })).toHaveAttribute('href', '/admin/customers');
    expect(screen.getByRole('link', { name: /carteira da empresa/i })).toHaveAttribute('href', '/admin/company-wallet');
  });
});
