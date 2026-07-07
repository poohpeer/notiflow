import type { NotificationStatus } from '../api/types';
import { statusLabel } from '../lib/format';

// Central status -> Tailwind color map. Referenced by badges and KPI cards.
const STATUS_COLORS: Record<NotificationStatus, string> = {
  SENT: 'bg-green-100 text-green-800 ring-green-600/20',
  QUEUED: 'bg-blue-100 text-blue-800 ring-blue-600/20',
  PROCESSING: 'bg-blue-100 text-blue-800 ring-blue-600/20',
  ACCEPTED: 'bg-slate-100 text-slate-700 ring-slate-500/20',
  FAILED_RETRYABLE: 'bg-amber-100 text-amber-800 ring-amber-600/20',
  FAILED_PERMANENT: 'bg-red-100 text-red-800 ring-red-600/20',
  DEAD_LETTERED: 'bg-red-100 text-red-800 ring-red-600/20',
};

export function statusColor(status: NotificationStatus): string {
  return STATUS_COLORS[status];
}

export function StatusBadge({ status }: { status: NotificationStatus }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset ${STATUS_COLORS[status]}`}
    >
      {statusLabel(status)}
    </span>
  );
}
