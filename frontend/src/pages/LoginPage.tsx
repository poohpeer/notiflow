import { useState, type FormEvent } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { InputField } from '../components/Field';
import { Logo } from '../components/Logo';
import { useCurrentUser, useLogin } from '../hooks/useAuth';

export function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const navigate = useNavigate();
  const location = useLocation();
  const { data: currentUser, isLoading } = useCurrentUser();
  const loginMutation = useLogin();

  // Where the user was headed before ProtectedRoute bounced them here.
  const from = (location.state as { from?: string } | null)?.from ?? '/';

  if (!isLoading && currentUser) {
    return <Navigate to={from} replace />;
  }

  function onSubmit(event: FormEvent) {
    event.preventDefault();
    loginMutation.mutate(
      { username, password },
      { onSuccess: () => navigate(from, { replace: true }) },
    );
  }

  const error = loginMutation.error;
  const message = error
    ? 'status' in error && (error as { status: number }).status === 401
      ? 'Invalid username or password'
      : error.message
    : null;

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <div className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
        <Logo className="mx-auto mb-8 h-10 w-auto" />

        <form onSubmit={onSubmit} className="space-y-4">
          <InputField
            id="username"
            label="Username"
            required
            autoFocus
            autoComplete="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
          <InputField
            id="password"
            label="Password"
            type="password"
            required
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          {message && (
            <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{message}</p>
          )}

          <button
            type="submit"
            disabled={loginMutation.isPending}
            className="w-full rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-60"
          >
            {loginMutation.isPending ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
      </div>
    </div>
  );
}
