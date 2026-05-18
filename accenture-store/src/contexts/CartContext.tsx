import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import type { CartItem, Product } from '../api/types';
import { useAuth } from './AuthContext';

interface CartContextValue {
  items: CartItem[];
  addItem: (product: Product, quantity?: number) => void;
  updateQuantity: (sku: string, quantity: number) => void;
  removeItem: (sku: string) => void;
  clear: () => void;
  totalItems: number;
  subtotal: number;
}

const CartContext = createContext<CartContextValue | null>(null);
const STORAGE_PREFIX = 'cart:';

const loadFromStorage = (key: string): CartItem[] => {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as CartItem[]) : [];
  } catch {
    return [];
  }
};

export function CartProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const storageKey = `${STORAGE_PREFIX}${user?.customerId || 'guest'}`;

  const [items, setItems] = useState<CartItem[]>(() => loadFromStorage(storageKey));

  useEffect(() => {
    setItems(loadFromStorage(storageKey));
  }, [storageKey]);

  useEffect(() => {
    localStorage.setItem(storageKey, JSON.stringify(items));
  }, [items, storageKey]);

  const addItem = (product: Product, quantity = 1) => {
    setItems((prev) => {
      const sku = product.sku || product.id;
      if (!sku) return prev;
      const idx = prev.findIndex((p) => p.sku === sku);
      if (idx >= 0) {
        const copy = [...prev];
        copy[idx] = { ...copy[idx], quantity: copy[idx].quantity + quantity };
        return copy;
      }
      return [
        ...prev,
        {
          sku,
          name: product.name,
          unitPrice: product.price ?? product.unitPrice ?? product.basePrice ?? 0,
          imageUrl: product.imageUrl,
          quantity,
        },
      ];
    });
  };

  const updateQuantity = (sku: string, quantity: number) =>
    setItems((prev) =>
      prev
        .map((it) => (it.sku === sku ? { ...it, quantity: Math.max(0, quantity) } : it))
        .filter((it) => it.quantity > 0),
    );

  const removeItem = (sku: string) => setItems((prev) => prev.filter((it) => it.sku !== sku));
  const clear = () => setItems([]);

  const value = useMemo<CartContextValue>(() => {
    const totalItems = items.reduce((s, it) => s + it.quantity, 0);
    const subtotal = items.reduce((s, it) => s + it.quantity * it.unitPrice, 0);
    return { items, addItem, updateQuantity, removeItem, clear, totalItems, subtotal };
  }, [items]);

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export const useCart = (): CartContextValue => {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used within CartProvider');
  return ctx;
};
