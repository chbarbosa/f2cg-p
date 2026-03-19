import { useState } from 'react';
import { login, register, verifyAccount } from '../api/auth';
import { useAuthStore } from '../store/authStore';

export function AuthForm() {
  const [tab, setTab] = useState<'login' | 'register'>('login');
  const [step, setStep] = useState<'auth' | 'verify'>('auth');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const setAuth = useAuthStore((s) => s.login);
  const setPendingEmail = useAuthStore((s) => s.setPendingEmail);

  async function handleAuthSubmit(e: React.SyntheticEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (tab === 'login') {
        const res = await login(email, password);
        setAuth(res.playerId, res.token, email);
      } else {
        await register(email, password);
        setPendingEmail(email);
        setStep('verify');
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Something went wrong');
    } finally {
      setLoading(false);
    }
  }

  async function handleVerifySubmit(e: React.SyntheticEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await verifyAccount(email, code);
      setAuth(res.playerId, res.token, email);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Something went wrong');
    } finally {
      setLoading(false);
    }
  }

  if (step === 'verify') {
    return (
      <div style={styles.card}>
        <h1 style={styles.title}>F2CG</h1>
        <p style={styles.hint}>A verification code was sent to <strong>{email}</strong></p>
        <form onSubmit={handleVerifySubmit} style={styles.form}>
          <input
            style={styles.input}
            placeholder="5-digit code"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            maxLength={5}
            pattern="\d{5}"
            required
            autoFocus
          />
          {error && <p style={styles.error}>{error}</p>}
          <button style={styles.button} type="submit" disabled={loading}>
            {loading ? '...' : 'Verify'}
          </button>
          <button
            style={styles.linkButton}
            type="button"
            onClick={() => { setStep('auth'); setError(''); setCode(''); }}
          >
            Back
          </button>
        </form>
      </div>
    );
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
      <form onSubmit={handleAuthSubmit} style={styles.form}>
        <input
          style={styles.input}
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
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
  hint: { margin: 0, color: '#a6adc8', fontSize: '0.85rem', textAlign: 'center' },
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
  linkButton: {
    padding: '0.4rem',
    background: 'none',
    border: 'none',
    color: '#a6adc8',
    fontSize: '0.85rem',
    cursor: 'pointer',
    textAlign: 'center',
  },
};