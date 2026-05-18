import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../../theme/theme';
import { SnackbarProvider } from '../../contexts/SnackbarContext';
import AdminProducts from './AdminProducts';

vi.mock('../../api/products', () => ({
  listProducts: vi.fn(),
  createProduct: vi.fn(),
  updateProduct: vi.fn(),
  deleteProduct: vi.fn(),
}));

import { listProducts, createProduct, updateProduct, deleteProduct } from '../../api/products';
const listMock = listProducts as unknown as ReturnType<typeof vi.fn>;
const createMock = createProduct as unknown as ReturnType<typeof vi.fn>;
const updateMock = updateProduct as unknown as ReturnType<typeof vi.fn>;
const deleteMock = deleteProduct as unknown as ReturnType<typeof vi.fn>;

function setup() {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <MemoryRouter>
        <SnackbarProvider>
          <AdminProducts />
        </SnackbarProvider>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

const product = {
  id: 'p1', sku: 'SKU1', name: 'A', description: 'd', category: 'C',
  basePrice: 10, stockQuantity: 5, imageUrl: 'http://x/img.png',
};

describe('AdminProducts', () => {
  beforeEach(() => {
    listMock.mockReset();
    createMock.mockReset();
    updateMock.mockReset();
    deleteMock.mockReset();
  });

  it('renderiza lista de produtos', async () => {
    listMock.mockResolvedValue([product]);
    setup();
    expect(await screen.findByText('A')).toBeInTheDocument();
    expect(screen.getByText('SKU1')).toBeInTheDocument();
  });

  it('vazio mostra mensagem', async () => {
    listMock.mockResolvedValue([]);
    setup();
    expect(await screen.findByText('Sem produtos cadastrados.')).toBeInTheDocument();
  });

  it('erro mostra alert', async () => {
    listMock.mockRejectedValue(new Error('boom'));
    setup();
    expect(await screen.findByText('boom')).toBeInTheDocument();
  });

  it('botão Salvar fica desabilitado com form vazio', async () => {
    listMock.mockResolvedValue([]);
    const user = userEvent.setup();
    setup();
    await screen.findByText('Sem produtos cadastrados.');
    await user.click(screen.getByRole('button', { name: /novo produto/i }));
    expect(screen.getByRole('button', { name: 'Salvar' })).toBeDisabled();
  });

  it('campo de estoque ignora letras digitadas', async () => {
    listMock.mockResolvedValue([]);
    const user = userEvent.setup();
    setup();
    await screen.findByText('Sem produtos cadastrados.');
    await user.click(screen.getByRole('button', { name: /novo produto/i }));
    const estoque = screen.getByLabelText(/estoque/i) as HTMLInputElement;
    await user.type(estoque, 'abc10xyz');
    expect(estoque.value).toBe('10');
  });

  it('mostra erro de preço inválido após perder o foco', async () => {
    listMock.mockResolvedValue([]);
    const user = userEvent.setup();
    setup();
    await screen.findByText('Sem produtos cadastrados.');
    await user.click(screen.getByRole('button', { name: /novo produto/i }));
    await user.type(screen.getByLabelText(/preço base/i), '0');
    await user.tab();
    expect(await screen.findByText('Preço deve ser maior que zero')).toBeInTheDocument();
  });

  it('cria produto válido', async () => {
    listMock.mockResolvedValueOnce([]).mockResolvedValueOnce([product]);
    createMock.mockResolvedValue(product);
    const user = userEvent.setup();
    setup();
    await screen.findByText('Sem produtos cadastrados.');
    await user.click(screen.getByRole('button', { name: /novo produto/i }));
    await user.type(screen.getByLabelText(/sku/i), 'SKU1');
    await user.type(screen.getByLabelText(/nome/i), 'Notebook');
    await user.type(screen.getByLabelText(/descrição/i), 'd');
    await user.type(screen.getByLabelText(/categoria/i), 'Eletrônicos');
    await user.type(screen.getByLabelText(/preço base/i), '99,90');
    await user.type(screen.getByLabelText(/estoque/i), '10');

    await user.click(screen.getByRole('button', { name: 'Salvar' }));
    await waitFor(() => expect(createMock).toHaveBeenCalledWith(expect.objectContaining({
      sku: 'SKU1', name: 'Notebook', category: 'Eletrônicos', basePrice: 99.9, stockQuantity: 10,
    })));
  });

  it('editar produto preenche form e chama update', async () => {
    listMock.mockResolvedValueOnce([product]);
    updateMock.mockResolvedValue(product);
    const user = userEvent.setup();
    setup();
    await screen.findByText('A');
    await user.click(screen.getByRole('button', { name: /editar/i }));
    expect(screen.getByLabelText(/sku/i)).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Salvar' }));
    await waitFor(() => expect(updateMock).toHaveBeenCalled());
  });

  it('excluir confirma e chama delete', async () => {
    listMock.mockResolvedValueOnce([product]);
    deleteMock.mockResolvedValue(undefined);
    const user = userEvent.setup();
    setup();
    await screen.findByText('A');
    await user.click(screen.getByRole('button', { name: /excluir/i }));
    await user.click((await screen.findAllByRole('button', { name: 'Excluir' })).pop()!);
    await waitFor(() => expect(deleteMock).toHaveBeenCalledWith('p1'));
  });

  it('cancelar form fecha sem salvar', async () => {
    listMock.mockResolvedValue([]);
    const user = userEvent.setup();
    setup();
    await screen.findByText('Sem produtos cadastrados.');
    await user.click(screen.getByRole('button', { name: /novo produto/i }));
    await user.click(screen.getByRole('button', { name: 'Cancelar' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(createMock).not.toHaveBeenCalled();
  });

  it('erro no submit notifica', async () => {
    listMock.mockResolvedValueOnce([]);
    createMock.mockRejectedValue(new Error('400'));
    const user = userEvent.setup();
    setup();
    await screen.findByText('Sem produtos cadastrados.');
    await user.click(screen.getByRole('button', { name: /novo produto/i }));
    await user.type(screen.getByLabelText(/sku/i), 'S');
    await user.type(screen.getByLabelText(/nome/i), 'N');
    await user.type(screen.getByLabelText(/descrição/i), 'd');
    await user.type(screen.getByLabelText(/categoria/i), 'C');
    await user.type(screen.getByLabelText(/preço base/i), '10');
    await user.type(screen.getByLabelText(/estoque/i), '1');
    await user.click(screen.getByRole('button', { name: 'Salvar' }));
    expect(await screen.findByText('400')).toBeInTheDocument();
  });
});
