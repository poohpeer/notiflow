// Types mirroring notiflow-contracts. Kept in sync manually with the Java
// records/enums in notiflow-contracts/.../contracts.

export type NotificationChannel = 'EMAIL' | 'TELEGRAM' | 'SMS' | 'PUSH';

export type NotificationStatus =
  | 'ACCEPTED'
  | 'QUEUED'
  | 'PROCESSING'
  | 'SENT'
  | 'FAILED_RETRYABLE'
  | 'FAILED_PERMANENT'
  | 'DEAD_LETTERED';

export const CHANNELS: NotificationChannel[] = ['EMAIL', 'TELEGRAM', 'SMS', 'PUSH'];

export const STATUSES: NotificationStatus[] = [
  'ACCEPTED',
  'QUEUED',
  'PROCESSING',
  'SENT',
  'FAILED_RETRYABLE',
  'FAILED_PERMANENT',
  'DEAD_LETTERED',
];

// A notification in one of these states will not change again, so the detail
// page stops polling and the dashboard treats them as settled.
export const TERMINAL_STATUSES: ReadonlySet<NotificationStatus> = new Set<NotificationStatus>([
  'SENT',
  'FAILED_PERMANENT',
  'DEAD_LETTERED',
]);

export function isTerminal(status: NotificationStatus): boolean {
  return TERMINAL_STATUSES.has(status);
}

// Field limits mirror the @Size/@NotBlank constraints on NotificationRequest.
export const LIMITS = {
  recipient: 320,
  subject: 200,
  message: 4000,
} as const;

export interface NotificationRequest {
  channel: NotificationChannel;
  recipient: string;
  subject?: string;
  message: string;
  metadata?: Record<string, string>;
}

export interface NotificationAcceptedResponse {
  notificationId: string;
  status: NotificationStatus;
  statusUrl: string;
}

export interface NotificationStatusResponse {
  notificationId: string;
  channel: NotificationChannel;
  recipient: string;
  subject: string | null;
  message: string;
  metadata: Record<string, string> | null;
  status: NotificationStatus;
  attempts: number;
  lastFailureReason: string | null;
  createdAt: string; // ISO-8601
  updatedAt: string; // ISO-8601
}

// Normalized page shape the UI consumes, independent of how Spring serializes
// Page<T> (flat legacy fields vs nested { page: {...} }). See normalizePage().
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
