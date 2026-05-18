import { Navigate, useLocation } from 'react-router-dom';
import type { ReactElement } from 'react';
import { useAuth } from '../contexts/AuthContext';

interface PrivateRouteProps {
  children: ReactElement;
}

export default function PrivateRoute({ children }: PrivateRouteProps) {
  const location = useLocation();
  const { isAuthenticated, isAdmin } = useAuth();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  if (isAdmin) {
    return <Navigate to="/admin" replace />;
  }
  return children;
}
