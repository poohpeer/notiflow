import { afterEach, describe, expect, it, vi } from 'vitest';
import { promQuery, promQueryRange, promScalar } from './metrics';

function mockFetch(impl: (url: string) => { ok: boolean; status?: number; json: unknown }) {
  const fetchMock = vi.fn(async (url: string) => {
    const r = impl(url);
    return { ok: r.ok, status: r.status ?? 200, json: async () => r.json };
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('promQuery', () => {
  it('maps instant vectors to labelled series', async () => {
    mockFetch(() => ({
      ok: true,
      json: {
        status: 'success',
        data: { resultType: 'vector', result: [{ metric: { channel: 'EMAIL' }, value: [123, '5'] }] },
      },
    }));

    const series = await promQuery('up');

    expect(series).toEqual([{ labels: { channel: 'EMAIL' }, value: 5 }]);
  });

  it('encodes the expression in the query URL', async () => {
    const fetchMock = mockFetch(() => ({
      ok: true,
      json: { status: 'success', data: { resultType: 'vector', result: [] } },
    }));

    await promQuery('sum(rate(x[1m]))');

    expect(String(fetchMock.mock.calls[0][0])).toContain(encodeURIComponent('sum(rate(x[1m]))'));
  });
});

describe('promScalar', () => {
  it('returns the first value', async () => {
    mockFetch(() => ({
      ok: true,
      json: { status: 'success', data: { resultType: 'vector', result: [{ metric: {}, value: [1, '42'] }] } },
    }));
    expect(await promScalar('x')).toBe(42);
  });

  it('returns 0 when Prometheus has no data', async () => {
    mockFetch(() => ({
      ok: true,
      json: { status: 'success', data: { resultType: 'vector', result: [] } },
    }));
    expect(await promScalar('x')).toBe(0);
  });
});

describe('promFetch error handling', () => {
  it('throws on a non-ok HTTP status', async () => {
    mockFetch(() => ({ ok: false, status: 500, json: {} }));
    await expect(promQuery('x')).rejects.toThrow('Prometheus request failed (500)');
  });

  it('throws on a Prometheus error envelope', async () => {
    mockFetch(() => ({ ok: true, json: { status: 'error', error: 'bad query', data: { result: [] } } }));
    await expect(promQuery('x')).rejects.toThrow('bad query');
  });
});

describe('promQueryRange', () => {
  it('merges range values into rows keyed by timestamp', async () => {
    mockFetch(() => ({
      ok: true,
      json: {
        status: 'success',
        data: {
          resultType: 'matrix',
          result: [{ metric: {}, values: [[100, '1'], [200, '2']] }],
        },
      },
    }));

    const rows = await promQueryRange({ accepted: 'sum(rate(x[1m]))' }, { minutes: 5, step: 15 });

    expect(rows).toEqual([
      { t: 100, accepted: 1 },
      { t: 200, accepted: 2 },
    ]);
  });
});
