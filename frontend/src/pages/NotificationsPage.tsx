import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ChannelBadge } from '../components/ChannelBadge';
import { StatusBadge } from '../components/StatusBadge';
import { Pagination } from '../components/Pagination';
import { useNotificationsList, type ListFilters } from '../hooks/useNotifications';
import {
  CHANNELS,
  STATUSES,
  type NotificationChannel,
  type NotificationStatus,
} from '../api/types';
import { channelLabel, formatDateTime, statusLabel } from '../lib/format';

const PAGE_SIZE = 20;

export function NotificationsPage() {
  const [channel, setChannel] = useState<NotificationChannel | ''>('');
  const [status, setStatus] = useState<NotificationStatus | ''>('');
  const [page, setPage] = useState(0);

  const filters: ListFilters = {
    channel: channel || undefined,
    status: status || undefined,
    page,
    size: PAGE_SIZE,
  };
  const { data, isLoading, isError, error, isPlaceholderData } = useNotificationsList(filters);

  function resetFilters() {
    setChannel('');
    setStatus('');
    setPage(0);
  }

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">Notifications</h1>
        <Link
          to="/notifications/new"
          className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
        >
          New notification
        </Link>
      </div>

      <div className="mb-4 flex flex-wrap items-end gap-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        <label className="text-sm">
          <span className="mb-1 block font-medium text-slate-700">Channel</span>
          <select
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
            value={channel}
            onChange={(e) => {
              setChannel(e.target.value as NotificationChannel | '');
              setPage(0);
            }}
          >
            <option value="">All</option>
            {CHANNELS.map((c) => (
              <option key={c} value={c}>
                {channelLabel(c)}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm">
          <span className="mb-1 block font-medium text-slate-700">Status</span>
          <select
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value as NotificationStatus | '');
              setPage(0);
            }}
          >
            <option value="">All</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {statusLabel(s)}
              </option>
            ))}
          </select>
        </label>
        {(channel || status) && (
          <button
            className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-600 hover:bg-slate-50"
            onClick={resetFilters}
          >
            Clear
          </button>
        )}
      </div>

      <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-xs font-medium uppercase tracking-wide text-slate-500">
              <tr>
                <th className="px-4 py-3">Recipient</th>
                <th className="px-4 py-3">Channel</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Attempts</th>
                <th className="px-4 py-3">Created</th>
              </tr>
            </thead>
            <tbody className={`divide-y divide-slate-100 ${isPlaceholderData ? 'opacity-60' : ''}`}>
              {isLoading && (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-slate-400">
                    Loading…
                  </td>
                </tr>
              )}
              {isError && (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-red-600">
                    {(error as Error).message}
                  </td>
                </tr>
              )}
              {!isLoading && !isError && data && data.content.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-slate-400">
                    No notifications match these filters.
                  </td>
                </tr>
              )}
              {data?.content.map((n) => (
                <tr key={n.notificationId} className="hover:bg-slate-50">
                  <td className="px-4 py-3">
                    <Link
                      to={`/notifications/${n.notificationId}`}
                      className="font-medium text-blue-600 hover:text-blue-700"
                    >
                      {n.recipient}
                    </Link>
                  </td>
                  <td className="px-4 py-3">
                    <ChannelBadge channel={n.channel} />
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge status={n.status} />
                  </td>
                  <td className="px-4 py-3 tabular-nums text-slate-600">{n.attempts}</td>
                  <td className="px-4 py-3 text-slate-600">{formatDateTime(n.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {data && data.content.length > 0 && <Pagination page={data} onPageChange={setPage} />}
      </div>
    </div>
  );
}
