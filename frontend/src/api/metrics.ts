// Thin client over the Prometheus HTTP API (reached via the /prom Vite proxy).
// Kept separate from api/client.ts because Prometheus has its own envelope and
// does not use the notiflow ApiError error shape.

const PROM_BASE = (import.meta.env.VITE_PROM_BASE ?? '/prom').replace(/\/$/, '');

interface PromInstantResult {
  metric: Record<string, string>;
  value: [number, string];
}
interface PromRangeResult {
  metric: Record<string, string>;
  values: [number, string][];
}
interface PromResponse<R> {
  status: 'success' | 'error';
  data: { resultType: string; result: R[] };
  error?: string;
}

export interface Series {
  labels: Record<string, string>;
  value: number;
}

async function promFetch<R>(path: string): Promise<PromResponse<R>> {
  const res = await fetch(`${PROM_BASE}${path}`);
  if (!res.ok) {
    throw new Error(`Prometheus request failed (${res.status})`);
  }
  const json = (await res.json()) as PromResponse<R>;
  if (json.status !== 'success') {
    throw new Error(json.error || 'Prometheus query error');
  }
  return json;
}

// Instant query -> one Series per result vector element.
export async function promQuery(expr: string): Promise<Series[]> {
  const json = await promFetch<PromInstantResult>(
    `/api/v1/query?query=${encodeURIComponent(expr)}`,
  );
  return json.data.result.map((r) => ({
    labels: r.metric,
    value: toNum(r.value[1]),
  }));
}

// Convenience: instant query expected to return a single scalar sum. Returns 0
// when Prometheus has no data yet (fresh scrape / no traffic).
export async function promScalar(expr: string): Promise<number> {
  const series = await promQuery(expr);
  return series.length ? series[0].value : 0;
}

export interface RangePoint {
  t: number; // unix seconds
  [series: string]: number;
}

// Range query -> rows keyed by timestamp, one column per named series, ready
// for Recharts. `series` maps a PromQL expression to a column name.
export async function promQueryRange(
  series: Record<string, string>,
  opts: { minutes?: number; step?: number } = {},
): Promise<RangePoint[]> {
  const minutes = opts.minutes ?? 30;
  const step = opts.step ?? 15;
  const end = Math.floor(Date.now() / 1000);
  const start = end - minutes * 60;

  const byTime = new Map<number, RangePoint>();
  await Promise.all(
    Object.entries(series).map(async ([name, expr]) => {
      const qs = new URLSearchParams({
        query: expr,
        start: String(start),
        end: String(end),
        step: String(step),
      });
      const json = await promFetch<PromRangeResult>(`/api/v1/query_range?${qs.toString()}`);
      // Sum across any label dimensions the expr didn't already aggregate.
      for (const result of json.data.result) {
        for (const [ts, val] of result.values) {
          const row = byTime.get(ts) ?? { t: ts };
          row[name] = (row[name] ?? 0) + toNum(val);
          byTime.set(ts, row);
        }
      }
    }),
  );

  return [...byTime.values()].sort((a, b) => a.t - b.t);
}

function toNum(v: string): number {
  const n = Number(v);
  return Number.isFinite(n) ? n : 0;
}
