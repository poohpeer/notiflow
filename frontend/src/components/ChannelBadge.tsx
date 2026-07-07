import type { NotificationChannel } from '../api/types';
import { channelLabel } from '../lib/format';

const CHANNEL_COLORS: Record<NotificationChannel, string> = {
  EMAIL: 'bg-indigo-50 text-indigo-700 ring-indigo-600/20',
  TELEGRAM: 'bg-sky-50 text-sky-700 ring-sky-600/20',
  SMS: 'bg-teal-50 text-teal-700 ring-teal-600/20',
  PUSH: 'bg-violet-50 text-violet-700 ring-violet-600/20',
};

export function ChannelBadge({ channel }: { channel: NotificationChannel }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset ${CHANNEL_COLORS[channel]}`}
    >
      {channelLabel(channel)}
    </span>
  );
}
