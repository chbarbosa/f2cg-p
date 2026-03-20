import { useState } from 'react';
import { updateProfile } from '../api/player';
import { useAuthStore } from '../store/authStore';

interface Props {
  onDone: () => void;
}

export function ProfileSetup({ onDone }: Props) {
  const setProfile = useAuthStore((s) => s.setProfile);
  const [nickname, setNickname] = useState('');
  const [country, setCountry] = useState(
    () => navigator.language.split('-')[1]?.toUpperCase() ?? ''
  );
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.SyntheticEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await updateProfile(nickname || null, country || null);
      setProfile(nickname || null, country || null);
      onDone();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Something went wrong');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={styles.card}>
      <h2 style={styles.title}>Set up your profile</h2>
      <p style={styles.hint}>Tell us a bit about yourself before playing</p>
      <form onSubmit={handleSubmit} style={styles.form}>
        <input
          style={styles.input}
          placeholder="Nickname (max 20)"
          value={nickname}
          onChange={(e) => setNickname(e.target.value)}
          maxLength={20}
          required
          autoFocus
        />
        <input
          style={styles.input}
          placeholder="Country code (e.g. US)"
          value={country}
          onChange={(e) => setCountry(e.target.value.toUpperCase())}
          maxLength={2}
          required
        />
        {error && <p style={styles.error}>{error}</p>}
        <button style={styles.button} type="submit" disabled={loading}>
          {loading ? '...' : 'Save and Play'}
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
  title: { margin: 0, color: '#cdd6f4', fontSize: '1.2rem' },
  hint: { margin: 0, color: '#a6adc8', fontSize: '0.85rem' },
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
    background: '#a6e3a1',
    color: '#1e1e2e',
    fontWeight: 700,
    fontSize: '1rem',
    cursor: 'pointer',
  },
};