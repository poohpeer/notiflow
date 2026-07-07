import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { InputField, SelectField, TextareaField } from '../components/Field';
import { useToast } from '../components/Toast';
import { useCreateNotification } from '../hooks/useNotifications';
import { ApiError } from '../api/client';
import { CHANNELS, LIMITS, type NotificationChannel, type NotificationRequest } from '../api/types';
import { channelLabel } from '../lib/format';

interface MetaRow {
  key: string;
  value: string;
}

export function CreateNotificationPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const createMutation = useCreateNotification();

  const [channel, setChannel] = useState<NotificationChannel>('EMAIL');
  const [recipient, setRecipient] = useState('');
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const [metaRows, setMetaRows] = useState<MetaRow[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});

  function validate(): boolean {
    const next: Record<string, string> = {};
    if (!recipient.trim()) next.recipient = 'Recipient is required';
    else if (recipient.length > LIMITS.recipient)
      next.recipient = `Must be at most ${LIMITS.recipient} characters`;
    if (subject.length > LIMITS.subject)
      next.subject = `Must be at most ${LIMITS.subject} characters`;
    if (!message.trim()) next.message = 'Message is required';
    else if (message.length > LIMITS.message)
      next.message = `Must be at most ${LIMITS.message} characters`;
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  function buildMetadata(): Record<string, string> | undefined {
    const entries = metaRows
      .map((r) => [r.key.trim(), r.value] as const)
      .filter(([k]) => k.length > 0);
    return entries.length ? Object.fromEntries(entries) : undefined;
  }

  function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!validate()) return;

    const req: NotificationRequest = {
      channel,
      recipient: recipient.trim(),
      subject: subject.trim() || undefined,
      message,
      metadata: buildMetadata(),
    };
    // One key per submit, reused across React Query retries -> true idempotency.
    const idempotencyKey = crypto.randomUUID();

    createMutation.mutate(
      { req, idempotencyKey },
      {
        onSuccess: (res) => {
          toast.success(`Notification accepted (${res.status})`);
          navigate(`/notifications/${res.notificationId}`);
        },
        onError: (err) => {
          const msg = err instanceof ApiError ? err.message : 'Failed to create notification';
          toast.error(msg);
        },
      },
    );
  }

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-6 text-xl font-semibold text-slate-900">New notification</h1>
      <form onSubmit={onSubmit} className="space-y-4 rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <SelectField
          label="Channel"
          required
          value={channel}
          onChange={(e) => setChannel(e.target.value as NotificationChannel)}
        >
          {CHANNELS.map((c) => (
            <option key={c} value={c}>
              {channelLabel(c)}
            </option>
          ))}
        </SelectField>

        <InputField
          label="Recipient"
          required
          value={recipient}
          error={errors.recipient}
          maxLength={LIMITS.recipient}
          placeholder="user@example.com / +15551234567 / chat id"
          onChange={(e) => setRecipient(e.target.value)}
        />

        <InputField
          label="Subject"
          value={subject}
          error={errors.subject}
          maxLength={LIMITS.subject}
          hint="Optional"
          onChange={(e) => setSubject(e.target.value)}
        />

        <TextareaField
          label="Message"
          required
          value={message}
          error={errors.message}
          rows={5}
          maxLength={LIMITS.message}
          hint={`${message.length}/${LIMITS.message}`}
          onChange={(e) => setMessage(e.target.value)}
        />

        <div>
          <div className="mb-1 flex items-center justify-between">
            <span className="text-sm font-medium text-slate-700">Metadata</span>
            <button
              type="button"
              className="text-sm font-medium text-blue-600 hover:text-blue-700"
              onClick={() => setMetaRows((rows) => [...rows, { key: '', value: '' }])}
            >
              + Add row
            </button>
          </div>
          <div className="space-y-2">
            {metaRows.map((row, i) => (
              <div key={i} className="flex items-center gap-2">
                <input
                  className="w-1/3 rounded-md border border-slate-300 px-2 py-1.5 text-sm"
                  placeholder="key"
                  value={row.key}
                  onChange={(e) =>
                    setMetaRows((rows) => rows.map((r, j) => (j === i ? { ...r, key: e.target.value } : r)))
                  }
                />
                <input
                  className="flex-1 rounded-md border border-slate-300 px-2 py-1.5 text-sm"
                  placeholder="value"
                  value={row.value}
                  onChange={(e) =>
                    setMetaRows((rows) => rows.map((r, j) => (j === i ? { ...r, value: e.target.value } : r)))
                  }
                />
                <button
                  type="button"
                  className="px-2 text-slate-400 hover:text-red-600"
                  aria-label="Remove row"
                  onClick={() => setMetaRows((rows) => rows.filter((_, j) => j !== i))}
                >
                  ✕
                </button>
              </div>
            ))}
            {metaRows.length === 0 && (
              <p className="text-xs text-slate-400">No metadata. Add key/value rows if needed.</p>
            )}
          </div>
        </div>

        <div className="flex justify-end gap-3 pt-2">
          <button
            type="button"
            className="rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
            onClick={() => navigate('/notifications')}
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={createMutation.isPending}
            className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
          >
            {createMutation.isPending ? 'Submitting…' : 'Create notification'}
          </button>
        </div>
      </form>
    </div>
  );
}
