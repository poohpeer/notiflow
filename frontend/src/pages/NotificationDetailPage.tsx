import { Link, useParams } from 'react-router-dom';
import { ChannelBadge } from '../components/ChannelBadge';
import { StatusBadge } from '../components/StatusBadge';
import { useNotification } from '../hooks/useNotifications';
import { ApiError } from '../api/client';
import { isTerminal } from '../api/types';
import { formatDateTime } from '../lib/format';

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="grid grid-cols-3 gap-4 border-b border-slate-100 px-4 py-3 last:border-0">
      <dt className="text-sm font-medium text-slate-500">{label}</dt>
      <dd className="col-span-2 text-sm text-slate-900">{children}</dd>
    </div>
  );
}

export function NotificationDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data, isLoading, isError, error } = useNotification(id);

  if (isLoading) {
    return <div className="py-12 text-center text-slate-400">Loading…</div>;
  }

  if (isError) {
    const notFound = error instanceof ApiError && error.status === 404;
    return (
      <div className="mx-auto max-w-lg rounded-lg border border-slate-200 bg-white p-8 text-center shadow-sm">
        <h1 className="text-lg font-semibold text-slate-900">
          {notFound ? 'Notification not found' : 'Something went wrong'}
        </h1>
        <p className="mt-2 text-sm text-slate-500">
          {notFound ? `No notification with id ${id}.` : (error as Error).message}
        </p>
        <Link
          to="/notifications"
          className="mt-6 inline-block rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
        >
          Back to notifications
        </Link>
      </div>
    );
  }

  if (!data) return null;

  const live = !isTerminal(data.status);
  const metadata = data.metadata ?? {};
  const metaEntries = Object.entries(metadata);

  return (
    <div className="mx-auto max-w-3xl">
      <div className="mb-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link to="/notifications" className="text-sm text-blue-600 hover:text-blue-700">
            ← Notifications
          </Link>
          <StatusBadge status={data.status} />
          {live && (
            <span className="inline-flex items-center gap-1 text-xs text-slate-400">
              <span className="h-2 w-2 animate-pulse rounded-full bg-blue-400" /> live
            </span>
          )}
        </div>
      </div>

      <div className="rounded-lg border border-slate-200 bg-white shadow-sm">
        <dl>
          <Row label="ID">
            <span className="font-mono text-xs">{data.notificationId}</span>
          </Row>
          <Row label="Channel">
            <ChannelBadge channel={data.channel} />
          </Row>
          <Row label="Recipient">{data.recipient}</Row>
          <Row label="Subject">{data.subject || <span className="text-slate-400">—</span>}</Row>
          <Row label="Message">
            <span className="whitespace-pre-wrap">{data.message}</span>
          </Row>
          <Row label="Status">
            <StatusBadge status={data.status} />
          </Row>
          <Row label="Attempts">{data.attempts}</Row>
          {data.lastFailureReason && (
            <Row label="Last failure">
              <span className="text-red-600">{data.lastFailureReason}</span>
            </Row>
          )}
          <Row label="Metadata">
            {metaEntries.length === 0 ? (
              <span className="text-slate-400">—</span>
            ) : (
              <table className="w-full text-xs">
                <tbody>
                  {metaEntries.map(([k, v]) => (
                    <tr key={k} className="border-b border-slate-100 last:border-0">
                      <td className="py-1 pr-4 font-medium text-slate-600">{k}</td>
                      <td className="py-1 text-slate-900">{v}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </Row>
          <Row label="Created">{formatDateTime(data.createdAt)}</Row>
          <Row label="Updated">{formatDateTime(data.updatedAt)}</Row>
        </dl>
      </div>
    </div>
  );
}
