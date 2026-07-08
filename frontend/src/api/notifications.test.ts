import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  countByStatus,
  createNotification,
  listNotifications,
  normalizePage,
} from './notifications';
import type { NotificationRequest } from './types';

function mockFetch(body: unknown) {
  const fetchMock = vi.fn(async (_url: string, _init?: RequestInit) => ({
    ok: true,
    status: 200,
    statusText: '',
    text: async () => JSON.stringify(body),
  }));
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('normalizePage', () => {
  it('reads the flat Spring Data shape', () => {
    const page = normalizePage({
      content: [{ id: '1' }],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
    });
    expect(page.content).toHaveLength(1);
    expect(page.totalElements).toBe(1);
    expect(page.first).toBe(true);
    expect(page.last).toBe(true);
  });

  it('reads the nested PagedModel shape', () => {
    const page = normalizePage({
      content: [{ id: 'a' }, { id: 'b' }],
      page: { number: 1, totalPages: 3, totalElements: 50, size: 20 },
    });
    expect(page.number).toBe(1);
    expect(page.totalElements).toBe(50);
    expect(page.first).toBe(false);
    expect(page.last).toBe(false);
  });

  it('defaults safely for empty input', () => {
    const page = normalizePage(undefined);
    expect(page.content).toEqual([]);
    expect(page.totalElements).toBe(0);
    expect(page.first).toBe(true);
    expect(page.last).toBe(true);
  });
});

describe('createNotification', () => {
  it('POSTs to /v1/notifications with the idempotency key', async () => {
    const fetchMock = mockFetch({ notificationId: 'id', status: 'ACCEPTED', statusUrl: '/x' });
    const req: NotificationRequest = {
      channel: 'EMAIL',
      recipient: 'user@example.com',
      subject: 'Hi',
      message: 'Body',
      metadata: {},
    };

    await createNotification(req, 'key-9');

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/v1/notifications');
    expect((init as RequestInit).method).toBe('POST');
    expect((init as RequestInit).headers).toMatchObject({ 'Idempotency-Key': 'key-9' });
  });
});

describe('listNotifications', () => {
  it('builds the query string and normalizes the response', async () => {
    const fetchMock = mockFetch({
      content: [{ id: '1' }],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
    });

    const page = await listNotifications({
      channel: 'EMAIL',
      status: 'SENT',
      page: 0,
      size: 20,
    });

    const [url, init] = fetchMock.mock.calls[0];
    expect((init as RequestInit).method).toBe('POST');
    const qs = new URLSearchParams(String(url).split('?')[1]);
    expect(qs.get('channel')).toBe('EMAIL');
    expect(qs.get('status')).toBe('SENT');
    expect(qs.get('page')).toBe('0');
    expect(qs.get('size')).toBe('20');
    expect(qs.get('sort')).toBe('createdAt,desc');
    expect(page.content).toHaveLength(1);
  });
});

describe('countByStatus', () => {
  it('returns totalElements from a size-1 page', async () => {
    mockFetch({ content: [], totalElements: 7, totalPages: 7, number: 0, size: 1 });
    expect(await countByStatus('DEAD_LETTERED')).toBe(7);
  });
});
