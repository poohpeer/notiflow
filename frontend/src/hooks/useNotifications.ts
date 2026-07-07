import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
  useQueries,
} from '@tanstack/react-query';
import {
  countByStatus,
  createNotification,
  getNotification,
  listNotifications,
  type ListParams,
} from '../api/notifications';
import {
  isTerminal,
  STATUSES,
  type NotificationRequest,
  type NotificationStatus,
} from '../api/types';

export interface ListFilters {
  channel?: ListParams['channel'];
  status?: ListParams['status'];
  page: number;
  size: number;
}

const notificationKeys = {
  all: ['notifications'] as const,
  list: (f: ListFilters) => ['notifications', 'list', f] as const,
  detail: (id: string) => ['notifications', 'detail', id] as const,
  count: (status: NotificationStatus) => ['notifications', 'count', status] as const,
  total: ['notifications', 'count', 'total'] as const,
};

export function useNotificationsList(filters: ListFilters) {
  return useQuery({
    queryKey: notificationKeys.list(filters),
    queryFn: () =>
      listNotifications({
        channel: filters.channel,
        status: filters.status,
        page: filters.page,
        size: filters.size,
      }),
    placeholderData: keepPreviousData,
  });
}

export function useNotification(id: string | undefined) {
  return useQuery({
    queryKey: notificationKeys.detail(id ?? ''),
    queryFn: () => getNotification(id as string),
    enabled: !!id,
    // Poll while the worker/relay may still advance the status; stop on terminal.
    refetchInterval: (query) => {
      const data = query.state.data;
      return data && !isTerminal(data.status) ? 3000 : false;
    },
  });
}

export function useCreateNotification() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (vars: { req: NotificationRequest; idempotencyKey: string }) =>
      createNotification(vars.req, vars.idempotencyKey),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

// Current backlog: how many notifications are right now in each status.
export function useStatusCounts() {
  const queries = useQueries({
    queries: STATUSES.map((status) => ({
      queryKey: notificationKeys.count(status),
      queryFn: () => countByStatus(status),
      refetchInterval: 10_000,
    })),
  });
  return STATUSES.map((status, i) => ({
    status,
    count: queries[i].data ?? 0,
    isLoading: queries[i].isLoading,
  }));
}

export function useTotalCount() {
  return useQuery({
    queryKey: notificationKeys.total,
    queryFn: async () => {
      const page = await listNotifications({ page: 0, size: 1 });
      return page.totalElements;
    },
    refetchInterval: 10_000,
  });
}
