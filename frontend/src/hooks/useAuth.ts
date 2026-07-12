import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getCurrentUser, login, logout, type CurrentUser } from '../api/auth';
import { ApiError } from '../api/client';

const authKeys = {
  me: ['auth', 'me'] as const,
};

/**
 * Current session. A 401 is a valid answer ("logged out"), not an error, so it
 * resolves to null instead of throwing — anything else propagates.
 */
export function useCurrentUser() {
  return useQuery<CurrentUser | null>({
    queryKey: authKeys.me,
    queryFn: async () => {
      try {
        return await getCurrentUser();
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) return null;
        throw error;
      }
    },
    retry: false,
    staleTime: 60_000,
  });
}

export function useLogin() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (vars: { username: string; password: string }) =>
      login(vars.username, vars.password),
    onSuccess: (user) => {
      queryClient.setQueryData(authKeys.me, user);
    },
  });
}

export function useLogout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: logout,
    onSuccess: () => {
      // Drop every cached notification/metric — it belonged to the old session.
      queryClient.clear();
      queryClient.setQueryData(authKeys.me, null);
    },
  });
}
