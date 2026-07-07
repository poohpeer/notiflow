import { request } from './client';
import type {
  NotificationAcceptedResponse,
  NotificationChannel,
  NotificationRequest,
  NotificationStatus,
  NotificationStatusResponse,
  Page,
} from './types';

export function createNotification(
  req: NotificationRequest,
  idempotencyKey: string,
): Promise<NotificationAcceptedResponse> {
  return request<NotificationAcceptedResponse>('/v1/notifications', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify(req),
  });
}

export function getNotification(id: string): Promise<NotificationStatusResponse> {
  return request<NotificationStatusResponse>(`/v1/notifications/${id}`);
}

export interface ListParams {
  channel?: NotificationChannel;
  status?: NotificationStatus;
  page: number;
  size: number;
  sort?: string; // e.g. "createdAt,desc"
}

export async function listNotifications(
  params: ListParams,
): Promise<Page<NotificationStatusResponse>> {
  const qs = new URLSearchParams();
  if (params.channel) qs.set('channel', params.channel);
  if (params.status) qs.set('status', params.status);
  qs.set('page', String(params.page));
  qs.set('size', String(params.size));
  qs.set('sort', params.sort ?? 'createdAt,desc');

  const raw = await request<unknown>(`/v1/notifications/all?${qs.toString()}`, {
    method: 'POST',
  });
  return normalizePage<NotificationStatusResponse>(raw);
}

// Exact count of notifications in a given status without fetching rows: read
// totalElements from a size:1 page.
export async function countByStatus(status: NotificationStatus): Promise<number> {
  const page = await listNotifications({ status, page: 0, size: 1 });
  return page.totalElements;
}

// Spring Data serializes Page<T> either flat (legacy) or nested under `page`
// (PagedModel), depending on version/config. Normalize both to our Page<T>.
export function normalizePage<T>(raw: unknown): Page<T> {
  const r = (raw ?? {}) as Record<string, unknown>;
  const content = Array.isArray(r.content) ? (r.content as T[]) : [];

  const nested = r.page as Record<string, unknown> | undefined;
  if (nested && typeof nested === 'object') {
    const number = num(nested.number);
    const totalPages = num(nested.totalPages);
    return {
      content,
      totalElements: num(nested.totalElements),
      totalPages,
      number,
      size: num(nested.size),
      first: number <= 0,
      last: totalPages === 0 || number >= totalPages - 1,
    };
  }

  // Flat/legacy shape with fields at the top level.
  const number = num(r.number);
  const totalPages = num(r.totalPages);
  return {
    content,
    totalElements: num(r.totalElements),
    totalPages,
    number,
    size: num(r.size),
    first: typeof r.first === 'boolean' ? r.first : number <= 0,
    last:
      typeof r.last === 'boolean'
        ? r.last
        : totalPages === 0 || number >= totalPages - 1,
  };
}

function num(v: unknown): number {
  const n = typeof v === 'number' ? v : Number(v);
  return Number.isFinite(n) ? n : 0;
}
