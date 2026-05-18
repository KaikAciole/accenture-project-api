import { describe, it, expect, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import type { ReactNode } from 'react';
import { CartProvider, useCart } from './CartContext';
import { AuthProvider } from './AuthContext';
import type { Product } from '../api/types';

const wrapper = ({ children }: { children: ReactNode }) => (
  <AuthProvider>
    <CartProvider>{children}</CartProvider>
  </AuthProvider>
);

const productA: Product = { id: 'p1', sku: 'SKU-A', name: 'Camisa', price: 50 };
const productB: Product = { sku: 'SKU-B', name: 'Caneca', unitPrice: 25 };
const productNoSku: Product = { name: 'Produto inválido', price: 10 };

describe('CartContext', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('useCart fora do provider lança erro explicativo', () => {
    expect(() => renderHook(() => useCart())).toThrow(/useCart must be used within CartProvider/);
  });

  it('addItem insere o produto e soma totais', () => {
    const { result } = renderHook(() => useCart(), { wrapper });

    act(() => result.current.addItem(productA, 2));

    expect(result.current.items).toHaveLength(1);
    expect(result.current.items[0]).toMatchObject({ sku: 'SKU-A', name: 'Camisa', quantity: 2, unitPrice: 50 });
    expect(result.current.totalItems).toBe(2);
    expect(result.current.subtotal).toBe(100);
  });

  it('addItem do mesmo SKU soma a quantidade ao invés de duplicar', () => {
    const { result } = renderHook(() => useCart(), { wrapper });

    act(() => result.current.addItem(productA));
    act(() => result.current.addItem(productA, 3));

    expect(result.current.items).toHaveLength(1);
    expect(result.current.items[0].quantity).toBe(4);
  });

  it('addItem ignora produtos sem sku/id', () => {
    const { result } = renderHook(() => useCart(), { wrapper });
    act(() => result.current.addItem(productNoSku));
    expect(result.current.items).toHaveLength(0);
  });

  it('updateQuantity ajusta para o valor informado e remove quando chega a zero', () => {
    const { result } = renderHook(() => useCart(), { wrapper });

    act(() => result.current.addItem(productA, 2));
    act(() => result.current.updateQuantity('SKU-A', 5));
    expect(result.current.items[0].quantity).toBe(5);

    act(() => result.current.updateQuantity('SKU-A', 0));
    expect(result.current.items).toHaveLength(0);
  });

  it('updateQuantity normaliza valores negativos para 0 (e remove)', () => {
    const { result } = renderHook(() => useCart(), { wrapper });
    act(() => result.current.addItem(productA));
    act(() => result.current.updateQuantity('SKU-A', -5));
    expect(result.current.items).toHaveLength(0);
  });

  it('removeItem retira somente o SKU pedido', () => {
    const { result } = renderHook(() => useCart(), { wrapper });
    act(() => {
      result.current.addItem(productA);
      result.current.addItem(productB);
    });
    act(() => result.current.removeItem('SKU-A'));
    expect(result.current.items).toHaveLength(1);
    expect(result.current.items[0].sku).toBe('SKU-B');
  });

  it('clear esvazia o carrinho', () => {
    const { result } = renderHook(() => useCart(), { wrapper });
    act(() => {
      result.current.addItem(productA);
      result.current.addItem(productB);
    });
    act(() => result.current.clear());
    expect(result.current.items).toHaveLength(0);
    expect(result.current.totalItems).toBe(0);
    expect(result.current.subtotal).toBe(0);
  });

  it('persiste no localStorage com a chave do guest', () => {
    const { result } = renderHook(() => useCart(), { wrapper });
    act(() => result.current.addItem(productA, 2));
    const raw = localStorage.getItem('cart:guest');
    expect(raw).toBeTruthy();
    expect(JSON.parse(raw as string)).toEqual([
      expect.objectContaining({ sku: 'SKU-A', quantity: 2 }),
    ]);
  });

  it('recupera estado inicial do localStorage', () => {
    localStorage.setItem(
      'cart:guest',
      JSON.stringify([{ sku: 'SKU-A', name: 'Camisa', unitPrice: 50, quantity: 3 }]),
    );
    const { result } = renderHook(() => useCart(), { wrapper });
    expect(result.current.items).toHaveLength(1);
    expect(result.current.totalItems).toBe(3);
    expect(result.current.subtotal).toBe(150);
  });

  it('aceita JSON inválido no storage sem quebrar', () => {
    localStorage.setItem('cart:guest', '{invalid');
    const { result } = renderHook(() => useCart(), { wrapper });
    expect(result.current.items).toEqual([]);
  });
});
