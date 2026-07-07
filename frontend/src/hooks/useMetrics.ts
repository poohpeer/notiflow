import { useQuery } from '@tanstack/react-query';
import { promQuery, promQueryRange, promScalar } from '../api/metrics';
import { PROMQL } from '../lib/format';

// All metric hooks refetch on a 10s cadence and surface `isError` so the
// dashboard can degrade gracefully when Prometheus is unreachable.

export interface MetricTotals {
  accepted: number;
  sent: number;
  dlq: number;
  published: number;
}

export function useMetricTotals() {
  return useQuery<MetricTotals>({
    queryKey: ['prom', 'totals'],
    queryFn: async () => {
      const [accepted, sent, dlq, published] = await Promise.all([
        promScalar(PROMQL.totalAccepted),
        promScalar(PROMQL.totalSent),
        promScalar(PROMQL.totalDlq),
        promScalar(PROMQL.totalPublished),
      ]);
      return { accepted, sent, dlq, published };
    },
    refetchInterval: 10_000,
    retry: false,
  });
}

export interface ChannelCount {
  channel: string;
  sent: number;
}

export function useChannelBreakdown() {
  return useQuery<ChannelCount[]>({
    queryKey: ['prom', 'sentByChannel'],
    queryFn: async () => {
      const series = await promQuery(PROMQL.sentByChannel);
      return series
        .map((s) => ({ channel: s.labels.channel ?? 'unknown', sent: s.value }))
        .sort((a, b) => b.sent - a.sent);
    },
    refetchInterval: 10_000,
    retry: false,
  });
}

export function useThroughput(minutes = 30) {
  return useQuery({
    queryKey: ['prom', 'throughput', minutes],
    queryFn: () =>
      promQueryRange(
        { accepted: PROMQL.acceptedRate, sent: PROMQL.sentRate },
        { minutes },
      ),
    refetchInterval: 10_000,
    retry: false,
  });
}

export interface ServiceStatus {
  job: string;
  up: boolean;
}

export function useServiceHealth() {
  return useQuery<ServiceStatus[]>({
    queryKey: ['prom', 'up'],
    queryFn: async () => {
      const series = await promQuery(PROMQL.serviceUp);
      return series
        .map((s) => ({ job: s.labels.job ?? 'unknown', up: s.value === 1 }))
        .sort((a, b) => a.job.localeCompare(b.job));
    },
    refetchInterval: 10_000,
    retry: false,
  });
}
