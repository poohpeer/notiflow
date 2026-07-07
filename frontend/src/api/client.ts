// Minimal fetch wrapper for the notiflow-api REST calls. No dependencies.

const API_BASE = (import.meta.env.VITE_API_BASE ?? '/api').replace(/\/$/, '');

// Handled errors from ApiExceptionHandler carry a `message`:
//   { timestamp, status, error, message }
// Errors that bypass it (e.g. a bad path variable) use Spring's default shape
// with no `message`, just `error` + `path`:
//   { timestamp, status, error, path }
interface ApiErrorBody {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
}

export class ApiError extends Error {
  readonly status: number;
  readonly body: ApiErrorBody | null;

  constructor(status: number, message: string, body: ApiErrorBody | null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

async function parseBody(res: Response): Promise<unknown> {
  // 204 / empty body guard.
  if (res.status === 204) return null;
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
  });

  const body = await parseBody(res);

  if (!res.ok) {
    const errBody = (body && typeof body === 'object' ? (body as ApiErrorBody) : null);
    const message =
      errBody?.message || errBody?.error || res.statusText || `Request failed (${res.status})`;
    throw new ApiError(res.status, message, errBody);
  }

  return body as T;
}
