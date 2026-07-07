import type { NotificationChannel, NotificationStatus } from '../api/types';

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

// Human labels — enums are already readable, but normalize the SNAKE_CASE.
export function statusLabel(status: NotificationStatus): string {
  return status
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
}

export function channelLabel(channel: NotificationChannel): string {
  return channel.charAt(0) + channel.slice(1).toLowerCase();
}

export function formatNumber(n: number): string {
  return n.toLocaleString();
}

// PromQL used by the dashboard, mirroring the Grafana notiflow-overview board.
export const PROMQL = {
  totalAccepted: 'sum(notiflow_notifications_accepted_total)',
  totalSent: 'sum(notiflow_notifications_sent_total)',
  totalDlq: 'sum(notiflow_notifications_dlq_total)',
  totalPublished: 'sum(notiflow_outbox_published_total)',
  sentByChannel: 'sum by (channel) (notiflow_notifications_sent_total)',
  acceptedRate: 'sum(rate(notiflow_notifications_accepted_total[1m]))',
  sentRate: 'sum(rate(notiflow_notifications_sent_total[1m]))',
  serviceUp: 'up{job=~"notiflow-api|notiflow-worker|notiflow-relay"}',
} as const;

export const GRAFANA_DASHBOARD_URL = 'http://localhost:3000/d/notiflow-overview';
