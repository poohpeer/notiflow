import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useCurrentUser } from '../hooks/useAuth';

/**
 * Gate for everything behind the login. The session check is a real request to
 * /api/v1/auth/me — this only hides the UI, the api enforces auth on its own.
 */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { data: currentUser, isLoading } = useCurrentUser();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center text-sm text-slate-500">
        Loading…
      </div>
    );
  }

  if (!currentUser) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }

  return <>{children}</>;
}
