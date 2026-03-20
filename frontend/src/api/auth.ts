export interface AuthResponse {
  playerId: string;
  token: string;
  nickname: string | null;
  country: string | null;
}

export interface RegisterResponse {
  message: string;
}

async function postAuth<T>(url: string, body: object): Promise<T> {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || res.statusText);
  }
  return res.json();
}

export const login = (username: string, password: string) =>
  postAuth<AuthResponse>('/api/auth/login', { username, password });

export const register = (username: string, password: string) =>
  postAuth<RegisterResponse>('/api/auth/register', { username, password });

export const verifyAccount = (email: string, code: string) =>
  postAuth<AuthResponse>('/api/auth/verify', { email, code });