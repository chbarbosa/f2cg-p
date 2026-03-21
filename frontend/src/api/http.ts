import { ApiError } from './errors';
import { useAuthStore } from '../store/authStore';

function authHeaders(): Record<string, string> {
  const token = useAuthStore.getState().token;
  return token
    ? { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }
    : { 'Content-Type': 'application/json' };
}

async function extractError(res: Response): Promise<never> {
  const text = await res.text();
  let message = res.statusText;
  try { message = JSON.parse(text).message || message; } catch { message = text || message; }
  throw new ApiError(res.status, message);
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) return extractError(res);
  return res.json() as Promise<T>;
}

export async function get<T>(url: string): Promise<T> {
  const res = await fetch(url, { headers: authHeaders() });
  return handleResponse<T>(res);
}

export async function post<T>(url: string, body: object): Promise<T> {
  const res = await fetch(url, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(body),
  });
  return handleResponse<T>(res);
}

export async function put<T>(url: string, body: object): Promise<T> {
  const res = await fetch(url, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(body),
  });
  return handleResponse<T>(res);
}

export async function putVoid(url: string, body: object): Promise<void> {
  const res = await fetch(url, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(body),
  });
  if (!res.ok) return extractError(res);
}

export async function del(url: string): Promise<void> {
  const res = await fetch(url, { method: 'DELETE', headers: authHeaders() });
  if (!res.ok) return extractError(res);
}