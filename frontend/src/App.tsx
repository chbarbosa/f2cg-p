import { AuthForm } from './components/AuthForm';
import { useAuthStore } from './store/authStore';

export default function App() {
  const { username, playerId, logout } = useAuthStore();

  if (username) {
    return (
      <div style={styles.center}>
        <div style={styles.card}>
          <h2 style={styles.welcome}>Welcome, {username}!</h2>
          <p style={styles.sub}>Player ID: <code style={styles.code}>{playerId}</code></p>
          <button style={styles.button} onClick={logout}>Logout</button>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.center}>
      <AuthForm />
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  center: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    background: '#181825',
  },
  card: {
    background: '#1e1e2e',
    border: '1px solid #313244',
    borderRadius: 12,
    padding: '2rem',
    width: 320,
    display: 'flex',
    flexDirection: 'column',
    gap: '1rem',
    alignItems: 'center',
  },
  welcome: { margin: 0, color: '#a6e3a1', fontSize: '1.3rem' },
  sub: { margin: 0, color: '#a6adc8', fontSize: '0.85rem', textAlign: 'center' },
  code: {
    background: '#313244',
    padding: '2px 6px',
    borderRadius: 4,
    color: '#89dceb',
    fontSize: '0.8rem',
  },
  button: {
    padding: '0.5rem 1.5rem',
    borderRadius: 6,
    border: 'none',
    background: '#f38ba8',
    color: '#1e1e2e',
    fontWeight: 600,
    cursor: 'pointer',
  },
};