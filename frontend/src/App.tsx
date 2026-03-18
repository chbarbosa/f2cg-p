import { useState } from 'react';
import { AuthForm } from './components/AuthForm';
import { DeckBuilder } from './components/DeckBuilder';
import { DeckList } from './components/DeckList';
import { useAuthStore } from './store/authStore';

type View = 'home' | 'deckList' | 'deckBuilder';

export default function App() {
  const { username, playerId, logout } = useAuthStore();
  const [view, setView] = useState<View>('home');

  const handleLogout = () => {
    logout();
    setView('home');
  };

  const handleEditDeck = () => setView('deckBuilder');
  const handleListDecks = () => setView('deckList');

  if (username) {
    if (view === 'deckList') {
      return (
        <div style={styles.page}>
          <DeckList onNew={handleEditDeck} onEdit={handleEditDeck} />
        </div>
      );
    }

    if (view === 'deckBuilder') {
      return (
        <div style={styles.page}>
          <DeckBuilder onCancel={handleListDecks} onSaved={handleListDecks} />
        </div>
      );
    }

    return (
      <div style={styles.center}>
        <div style={styles.card}>
          <h2 style={styles.welcome}>Welcome, {username}!</h2>
          <p style={styles.sub}>Player ID: <code style={styles.code}>{playerId}</code></p>
          <button style={styles.deckBtn} onClick={handleListDecks}>My Decks</button>
          <button style={styles.logoutBtn} onClick={handleLogout}>Logout</button>
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
  page: {
    minHeight: '100vh',
    background: '#181825',
    padding: '2rem',
  },
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
  deckBtn: {
    padding: '0.5rem 1.5rem',
    borderRadius: 6,
    border: 'none',
    background: '#89b4fa',
    color: '#1e1e2e',
    fontWeight: 600,
    cursor: 'pointer',
    width: '100%',
  },
  logoutBtn: {
    padding: '0.5rem 1.5rem',
    borderRadius: 6,
    border: 'none',
    background: '#f38ba8',
    color: '#1e1e2e',
    fontWeight: 600,
    cursor: 'pointer',
    width: '100%',
  },
};