import { useState } from 'react';
import { updateProfile } from '../api/player';
import { useAuthStore } from '../store/authStore';
import { CountrySelect } from './CountrySelect';

interface Props {
  onBack: () => void;
}

export function ConfigScreen({ onBack }: Props) {
  const { nickname: savedNickname, country: savedCountry, setProfile } = useAuthStore();
  const [nickname, setNickname] = useState(savedNickname ?? '');
  const [country, setCountry] = useState(savedCountry ?? '');
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.SyntheticEvent) {
    e.preventDefault();
    setError('');
    setSaved(false);
    setLoading(true);
    try {
      await updateProfile(nickname || null, country || null);
      setProfile(nickname || null, country || null);
      setSaved(true);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Something went wrong');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.card}>
        <div style={styles.header}>
          <button style={styles.backBtn} onClick={onBack}>← Back</button>
          <h2 style={styles.title}>Config</h2>
        </div>
        <form onSubmit={handleSubmit} style={styles.form}>
          <label style={styles.label}>Nickname</label>
          <input
            style={styles.input}
            placeholder="Your nickname (max 20)"
            value={nickname}
            onChange={(e) => { setNickname(e.target.value); setSaved(false); }}
            maxLength={20}
          />
          <label style={styles.label}>Country</label>
          <CountrySelect
            value={country}
            onChange={(code) => { setCountry(code); setSaved(false); }}
            inputStyle={styles.input}
          />
          {error && <p style={styles.error}>{error}</p>}
          {saved && <p style={styles.success}>Saved!</p>}
          <button style={styles.button} type="submit" disabled={loading}>
            {loading ? '...' : 'Save'}
          </button>
        </form>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  page: { display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: '#181825' },
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
  header: { display: 'flex', alignItems: 'center', gap: '1rem' },
  backBtn: {
    background: 'none',
    border: 'none',
    color: '#89b4fa',
    cursor: 'pointer',
    fontSize: '0.9rem',
    padding: 0,
  },
  title: { margin: 0, color: '#cdd6f4', fontSize: '1.2rem' },
  form: { display: 'flex', flexDirection: 'column', gap: '0.5rem' },
  label: { color: '#a6adc8', fontSize: '0.85rem' },
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
  success: { margin: 0, color: '#a6e3a1', fontSize: '0.85rem' },
  button: {
    padding: '0.6rem',
    borderRadius: 6,
    border: 'none',
    background: '#89b4fa',
    color: '#1e1e2e',
    fontWeight: 600,
    fontSize: '1rem',
    cursor: 'pointer',
    marginTop: '0.5rem',
  },
};