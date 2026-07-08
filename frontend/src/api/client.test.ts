import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, request } from './client';

type FakeResponse = {
  ok: boolean;
  status: number;
  statusText?: string;
  body: string;
};

function mockFetch(res: FakeResponse) {
  const fetchMock = vi.fn(async (_url: string, _init?: RequestInit) => ({
    ok: res.ok,
    status: res.status,
    statusText: res.statusText ?? '',
    text: async () => res.body,
  }));
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('request', () => {
  it('prefixes /api, sets JSON content type, and parses the body', async () => {
    const fetchMock = mockFetch({ ok: true, status: 200, body: '{"a":1}' });

    const result = await request<{ a: number }>('/v1/ping');

    expect(result).toEqual({ a: 1 });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/v1/ping');
    expect((init as RequestInit).headers).toMatchObject({ 'Content-Type': 'application/json' });
  });

  it('returns null for an empty body', async () => {
    mockFetch({ ok: true, status: 200, body: '' });
    expect(await request('/v1/empty')).toBeNull();
  });

  it('returns null for 204 No Content', async () => {
    mockFetch({ ok: true, status: 204, body: 'ignored' });
    expect(await request('/v1/none')).toBeNull();
  });

  it('throws ApiError using the handler message on error responses', async () => {
    mockFetch({
      ok: false,
      status: 409,
      body: '{"status":409,"error":"Conflict","message":"payload differs"}',
    });

    const error = await request('/v1/x').catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    const err = error as ApiError;
    expect(err.status).toBe(409);
    expect(err.message).toBe('payload differs');
    expect(err.body?.error).toBe('Conflict');
  });

  it('falls back to error/statusText when no message is present', async () => {
    mockFetch({
      ok: false,
      status: 404,
      statusText: 'Not Found',
      body: '{"status":404,"error":"Not Found","path":"/x"}',
    });

    await expect(request('/v1/missing')).rejects.toMatchObject({
      status: 404,
      message: 'Not Found',
    });
  });
});
