import { useState } from 'react';
import { login, register } from '../api/auth';
import { useAuthStore } from '../store/authStore';

export function AuthForm() {
  const [tab, setTab] = useState<'login' | 'register'>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const setAuth = useAuthStore((s) => s.login);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const fn = tab === 'login' ? login : register;
      const res = await fn(username, password);
      setAuth(res.playerId, res.token, username);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Something went wrong');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={styles.card}>
      <h1 style={styles.title}>F2CG</h1>
      <div style={styles.tabs}>
        <button
          style={{ ...styles.tab, ...(tab === 'login' ? styles.activeTab : {}) }}
          onClick={() => { setTab('login'); setError(''); }}
        >
          Login
        </button>
        <button
          style={{ ...styles.tab, ...(tab === 'register' ? styles.activeTab : {}) }}
          onClick={() => { setTab('register'); setError(''); }}
        >
          Register
        </button>
      </div>
      <form onSubmit={handleSubmit} style={styles.form}>
        <input
          style={styles.input}
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
          autoFocus
        />
        <input
          style={styles.input}
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        {error && <p style={styles.error}>{error}</p>}
        <button style={styles.button} type="submit" disabled={loading}>
          {loading ? '...' : tab === 'login' ? 'Login' : 'Register'}
        </button>
      </form>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  card: {
    background: '#1e1e2e',
    border: '1px solid #313244',
    borderRadius: 12,
    padding: '2rem',
    width: 320,
    display: 'flex',
    flexDirection: 'column',
    gap: '1rem',
  },
  title: { margin: 0, textAlign: 'center', color: '#cdd6f4', fontSize: '1.5rem' },
  tabs: { display: 'flex', gap: 8 },
  tab: {
    flex: 1,
    padding: '0.5rem',
    background: '#313244',
    border: 'none',
    borderRadius: 6,
    color: '#a6adc8',
    cursor: 'pointer',
    fontSize: '0.9rem',
  },
  activeTab: { background: '#89b4fa', color: '#1e1e2e', fontWeight: 600 },
  form: { display: 'flex', flexDirection: 'column', gap: '0.75rem' },
  input: {
    padding: '0.6rem 0.8rem',
    borderRadius: 6,
    border: '1px solid #45475a',
    background: '#313244',
    color: '#cdd6f4',
    fontSize: '0.95rem',
    outline: 'none',
  },
  error: { margin: 0, color: '#f38ba8', fontSize: '0.85rem' },
  button: {
    padding: '0.6rem',
    borderRadius: 6,
    border: 'none',
    background: '#89b4fa',
    color: '#1e1e2e',
    fontWeight: 600,
    fontSize: '0.95rem',
    cursor: 'pointer',
  },
};