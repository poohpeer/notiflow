import type { ReactNode } from 'react';
import { formatNumber } from '../lib/format';

interface KpiCardProps {
  label: ReactNode;
  value: number;
  accent?: string; // Tailwind classes for the value color
  loading?: boolean;
}

export function KpiCard({ label, value, accent = 'text-slate-900', loading }: KpiCardProps) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</div>
      <div className={`mt-2 text-2xl font-semibold tabular-nums ${accent}`}>
        {loading ? <span className="text-slate-300">—</span> : formatNumber(value)}
      </div>
    </div>
  );
}
