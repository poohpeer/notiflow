import { request } from './client';

export interface CurrentUser {
  username: string;
}

export function login(username: string, password: string): Promise<CurrentUser> {
  return request<CurrentUser>('/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
}

export function logout(): Promise<null> {
  return request<null>('/v1/auth/logout', { method: 'POST' });
}

// 401 here simply means "not logged in" — callers treat it as an empty session.
export function getCurrentUser(): Promise<CurrentUser> {
  return request<CurrentUser>('/v1/auth/me');
}
