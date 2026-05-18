import { Navigate, useLocation } from 'react-router-dom';
import { useEffect, useRef, type ReactElement } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useSnackbar } from '../contexts/SnackbarContext';

interface AdminRouteProps {
  children: ReactElement;
}

export default function AdminRoute({ children }: AdminRouteProps) {
  const location = useLocation();
  const { isAuthenticated, isAdmin } = useAuth();
  const { notify } = useSnackbar();
  const warned = useRef(false);

  useEffect(() => {
    if (isAuthenticated && !isAdmin && !warned.current) {
      warned.current = true;
      notify('Você não tem permissão pra essa área', 'error');
    }
  }, [isAuthenticated, isAdmin, notify]);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  if (!isAdmin) {
    return <Navigate to="/" replace />;
  }
  return children;
}
