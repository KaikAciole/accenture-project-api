import type { ReactElement, ReactNode } from 'react';
import { render, type RenderOptions, type RenderResult } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { buildTheme } from '../theme/theme';
import { SnackbarProvider } from '../contexts/SnackbarContext';
import { AuthProvider } from '../contexts/AuthContext';
import { CartProvider } from '../contexts/CartContext';

interface ProvidersProps {
  children: ReactNode;
  route?: string;
}

export function AllProviders({ children, route = '/' }: ProvidersProps) {
  const theme = buildTheme('light');
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <MemoryRouter initialEntries={[route]}>
        <SnackbarProvider>
          <AuthProvider>
            <CartProvider>{children}</CartProvider>
          </AuthProvider>
        </SnackbarProvider>
      </MemoryRouter>
    </ThemeProvider>
  );
}

interface RenderWithProvidersOptions extends Omit<RenderOptions, 'wrapper'> {
  route?: string;
}

export function renderWithProviders(
  ui: ReactElement,
  { route = '/', ...options }: RenderWithProvidersOptions = {},
): RenderResult {
  return render(ui, {
    wrapper: ({ children }) => <AllProviders route={route}>{children}</AllProviders>,
    ...options,
  });
}

interface ThemedRouterOptions extends Omit<RenderOptions, 'wrapper'> {
  route?: string;
}

export function renderThemedRouter(
  ui: ReactElement,
  { route = '/', ...options }: ThemedRouterOptions = {},
): RenderResult {
  return render(ui, {
    wrapper: ({ children }) => (
      <ThemeProvider theme={buildTheme('light')}>
        <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>
      </ThemeProvider>
    ),
    ...options,
  });
}

export * from '@testing-library/react';
