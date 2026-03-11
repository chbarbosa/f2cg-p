export interface AuthResponse {
  playerId: string;
  token: string;
}

async function post(url: string, body: object): Promise<AuthResponse> {
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
  post('/api/auth/login', { username, password });

export const register = (username: string, password: string) =>
  post('/api/auth/register', { username, password });