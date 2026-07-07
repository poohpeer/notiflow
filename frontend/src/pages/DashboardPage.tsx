import { Link } from 'react-router-dom';
import { KpiCard } from '../components/KpiCard';
import { ServiceHealth } from '../components/ServiceHealth';
import { ThroughputChart } from '../components/ThroughputChart';
import { statusColor } from '../components/StatusBadge';
import { useStatusCounts, useTotalCount } from '../hooks/useNotifications';
import {
  useChannelBreakdown,
  useMetricTotals,
  useServiceHealth,
  useThroughput,
} from '../hooks/useMetrics';
import { formatNumber, statusLabel, GRAFANA_DASHBOARD_URL } from '../lib/format';

function Section({ title, right, children }: { title: string; right?: React.ReactNode; children: React.ReactNode }) {
  return (
    <section className="mb-8">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">{title}</h2>
        {right}
      </div>
      {children}
    </section>
  );
}

function MetricsUnavailable() {
  return (
    <div className="rounded-lg border border-dashed border-slate-300 bg-white p-4 text-sm text-slate-400">
      Metrics unavailable — is Prometheus running on :9090?
    </div>
  );
}

export function DashboardPage() {
  const statusCounts = useStatusCounts();
  const total = useTotalCount();

  const totals = useMetricTotals();
  const channels = useChannelBreakdown();
  const throughput = useThroughput(30);
  const health = useServiceHealth();

  const metricsDown = totals.isError && channels.isError && throughput.isError && health.isError;

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">Dashboard</h1>
        <Link
          to="/notifications/new"
          className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
        >
          New notification
        </Link>
      </div>

      {/* Band 1 — current backlog by status (list API) */}
      <Section
        title="Current backlog"
        right={
          <span className="text-sm text-slate-500">
            Total: <span className="font-semibold text-slate-900">{formatNumber(total.data ?? 0)}</span>
          </span>
        }
      >
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {statusCounts.map(({ status, count, isLoading }) => (
            <KpiCard
              key={status}
              label={<span className={`rounded px-1.5 py-0.5 ${statusColor(status)}`}>{statusLabel(status)}</span>}
              value={count}
              loading={isLoading}
            />
          ))}
        </div>
      </Section>

      {/* Band 2 — flow totals + throughput (Prometheus) */}
      <Section
        title="Delivery flow"
        right={
          <a
            href={GRAFANA_DASHBOARD_URL}
            target="_blank"
            rel="noreferrer"
            className="text-sm text-blue-600 hover:text-blue-700"
          >
            Open full metrics in Grafana ↗
          </a>
        }
      >
        {metricsDown ? (
          <MetricsUnavailable />
        ) : (
          <>
            <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
              <KpiCard label="Accepted" value={totals.data?.accepted ?? 0} loading={totals.isLoading} accent="text-slate-900" />
              <KpiCard label="Sent" value={totals.data?.sent ?? 0} loading={totals.isLoading} accent="text-green-700" />
              <KpiCard label="Dead-lettered" value={totals.data?.dlq ?? 0} loading={totals.isLoading} accent="text-red-700" />
              <KpiCard label="Outbox published" value={totals.data?.published ?? 0} loading={totals.isLoading} accent="text-blue-700" />
            </div>

            <div className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-3">
              <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm lg:col-span-2">
                <h3 className="mb-2 text-sm font-medium text-slate-700">Throughput (per second, last 30m)</h3>
                <ThroughputChart data={throughput.data ?? []} />
              </div>
              <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
                <h3 className="mb-2 text-sm font-medium text-slate-700">Sent by channel</h3>
                {channels.data && channels.data.length > 0 ? (
                  <table className="w-full text-sm">
                    <tbody>
                      {channels.data.map((c) => (
                        <tr key={c.channel} className="border-b border-slate-100 last:border-0">
                          <td className="py-1.5 text-slate-600">{c.channel}</td>
                          <td className="py-1.5 text-right font-medium tabular-nums text-slate-900">
                            {formatNumber(c.sent)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                ) : (
                  <p className="text-sm text-slate-400">No sends recorded yet.</p>
                )}
              </div>
            </div>
          </>
        )}
      </Section>

      {/* Band 3 — service health (Prometheus up{}) */}
      <Section title="Service health">
        {health.isError ? <MetricsUnavailable /> : <ServiceHealth services={health.data ?? []} />}
      </Section>
    </div>
  );
}
