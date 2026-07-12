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

function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  // Spring Security hands out the CSRF token in a readable XSRF-TOKEN cookie and
  // expects it back in this header on every mutating request.
  const csrfToken = readCookie('XSRF-TOKEN');

  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    // The session id lives in an HttpOnly cookie; without this it is not sent.
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {}),
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
