import { useMemo } from 'react';
import { Routes, Route, useLocation } from 'react-router-dom';
import { ThemeProvider, CssBaseline, Box } from '@mui/material';
import { buildTheme } from './theme/theme';
import { ThemeModeProvider, useThemeMode } from './contexts/ThemeModeContext';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { CartProvider } from './contexts/CartContext';
import { SnackbarProvider } from './contexts/SnackbarContext';

import Header from './components/Header';
import MinimalHeader from './components/MinimalHeader';
import SecondaryNav from './components/SecondaryNav';
import Footer from './components/Footer';
import PrivateRoute from './components/PrivateRoute';
import AdminRoute from './components/AdminRoute';
import AssistantChat from './components/AssistantChat';

import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import Home from './pages/Home';
import ProductDetail from './pages/ProductDetail';
import Cart from './pages/Cart';
import Checkout from './pages/Checkout';
import Orders from './pages/Orders';
import OrderDetail from './pages/OrderDetail';
import Wallet from './pages/Wallet';
import Addresses from './pages/Addresses';
import Profile from './pages/Profile';
import AdminDashboard from './pages/admin/AdminDashboard';
import AdminProducts from './pages/admin/AdminProducts';
import AdminOrders from './pages/admin/AdminOrders';
import AdminCustomers from './pages/admin/AdminCustomers';
import AdminCompanyWallet from './pages/admin/AdminCompanyWallet';

const AUTH_PAGES = new Set(['/login', '/register', '/forgot-password', '/reset-password']);

function Chrome() {
  const { isAuthenticated, isAdmin } = useAuth();
  const { pathname } = useLocation();

  if (AUTH_PAGES.has(pathname)) return null;
  if (isAuthenticated) {
    return (
      <>
        <Header />
        {!isAdmin && <SecondaryNav />}
      </>
    );
  }
  return <MinimalHeader />;
}

function AppFooter() {
  const { pathname } = useLocation();
  if (AUTH_PAGES.has(pathname)) return null;
  return <Footer />;
}

function ThemedApp() {
  const { mode } = useThemeMode();
  const theme = useMemo(() => buildTheme(mode), [mode]);

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <SnackbarProvider>
        <AuthProvider>
          <CartProvider>
            <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
              <Chrome />
              <Box component="main" sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                <Routes>
                  <Route path="/" element={<Home />} />
                  <Route path="/login" element={<Login />} />
                  <Route path="/register" element={<Register />} />
                  <Route path="/forgot-password" element={<ForgotPassword />} />
                  <Route path="/reset-password" element={<ResetPassword />} />
                  <Route path="/products/:id" element={<PrivateRoute><ProductDetail /></PrivateRoute>} />
                  <Route path="/cart" element={<PrivateRoute><Cart /></PrivateRoute>} />
                  <Route path="/checkout" element={<PrivateRoute><Checkout /></PrivateRoute>} />
                  <Route path="/orders" element={<PrivateRoute><Orders /></PrivateRoute>} />
                  <Route path="/orders/:id" element={<PrivateRoute><OrderDetail /></PrivateRoute>} />
                  <Route path="/wallet" element={<PrivateRoute><Wallet /></PrivateRoute>} />
                  <Route path="/addresses" element={<PrivateRoute><Addresses /></PrivateRoute>} />
                  <Route path="/profile" element={<PrivateRoute><Profile /></PrivateRoute>} />
                  <Route path="/admin" element={<AdminRoute><AdminDashboard /></AdminRoute>} />
                  <Route path="/admin/products" element={<AdminRoute><AdminProducts /></AdminRoute>} />
                  <Route path="/admin/orders" element={<AdminRoute><AdminOrders /></AdminRoute>} />
                  <Route path="/admin/customers" element={<AdminRoute><AdminCustomers /></AdminRoute>} />
                  <Route path="/admin/company-wallet" element={<AdminRoute><AdminCompanyWallet /></AdminRoute>} />
                </Routes>
              </Box>
              <AppFooter />
              <AssistantChat />
            </Box>
          </CartProvider>
        </AuthProvider>
      </SnackbarProvider>
    </ThemeProvider>
  );
}

export default function App() {
  return (
    <ThemeModeProvider>
      <ThemedApp />
    </ThemeModeProvider>
  );
}
