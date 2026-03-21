import { useEffect, useState } from 'react';
import { AuthForm } from './components/AuthForm';
import { ConfigScreen } from './components/ConfigScreen';
import { DeckBuilder } from './components/DeckBuilder';
import { DeckList } from './components/DeckList';
import { DeckSelector } from './components/DeckSelector';
import { ProfileSetup } from './components/ProfileSetup';
import { QueueWaiting } from './components/QueueWaiting';
import { useAuthStore } from './store/authStore';
import { useDeckStore } from './store/deckStore';
import { useQueueStore } from './store/queueStore';

type View = 'home' | 'deckList' | 'deckBuilder' | 'profileSetup' | 'deckSelector' | 'queueWaiting' | 'config';

export default function App() {
  const { username, playerId, nickname, logout } = useAuthStore();
  const { decks, fetchDecks } = useDeckStore();
  const { join, clearEntry } = useQueueStore();
  const [view, setView] = useState<View>('home');

  useEffect(() => {
    if (username && view === 'home') {
      fetchDecks();
    }
  }, [username, view, fetchDecks]);

  const handleLogout = () => {
    logout();
    clearEntry();
    setView('home');
  };

  const handleEditDeck = () => setView('deckBuilder');
  const handleListDecks = () => setView('deckList');
  const handleHome = () => setView('home');

  const handleReady = async (deckId: string) => {
    await join(deckId);
    setView('queueWaiting');
  };

  const hasPlayableDeck = decks.some(d => d.status === 'PLAYABLE');

  if (username) {
    if (view === 'deckList') {
      return (
        <div style={styles.page}>
          <DeckList onBack={handleHome} onNew={handleEditDeck} onEdit={handleEditDeck} />
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

    if (view === 'profileSetup') {
      return (
        <div style={styles.center}>
          <ProfileSetup onDone={() => setView('deckSelector')} />
        </div>
      );
    }

    if (view === 'config') {
      return <ConfigScreen onBack={handleHome} />;
    }

    if (view === 'deckSelector') {
      return (
        <div style={styles.page}>
          <DeckSelector
            onReady={handleReady}
            onBack={handleHome}
          />
        </div>
      );
    }

    if (view === 'queueWaiting') {
      return <QueueWaiting onCancelled={() => { clearEntry(); handleHome(); }} />;
    }

    return (
      <div style={styles.center}>
        <div style={styles.card}>
          <h2 style={styles.welcome}>Welcome, {username}!</h2>
          <p style={styles.sub}>Player ID: <code style={styles.code}>{playerId}</code></p>
          <div style={styles.playWrapper} title={!hasPlayableDeck ? 'You need at least one playable deck to battle' : undefined}>
            <button
              style={hasPlayableDeck ? styles.playBtn : { ...styles.playBtn, opacity: 0.4, cursor: 'not-allowed' }}
              disabled={!hasPlayableDeck}
              onClick={() => setView(nickname ? 'deckSelector' : 'profileSetup')}
            >
              Play
            </button>
          </div>
          <button style={styles.deckBtn} onClick={handleListDecks}>My Decks</button>
          <button style={styles.disabledBtn} disabled>Store</button>
          <button style={styles.deckBtn} onClick={() => setView('config')}>Config</button>
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
  playWrapper: { width: '100%' },
  playBtn: {
    padding: '0.5rem 1.5rem',
    borderRadius: 6,
    border: 'none',
    background: '#a6e3a1',
    color: '#1e1e2e',
    fontWeight: 700,
    fontSize: '1rem',
    cursor: 'pointer',
    width: '100%',
  },
  deckBtn: {
    padding: '0.5rem 1.5rem',
    borderRadius: 6,
    border: 'none',
    background: '#89b4fa',
    color: '#1e1e2e',
    fontWeight: 600,
    fontSize: '1rem',
    cursor: 'pointer',
    width: '100%',
  },
  disabledBtn: {
    padding: '0.5rem 1.5rem',
    borderRadius: 6,
    border: 'none',
    background: '#313244',
    color: '#585b70',
    fontWeight: 600,
    fontSize: '1rem',
    cursor: 'not-allowed',
    width: '100%',
    opacity: 0.6,
  },
  logoutBtn: {
    padding: '0.5rem 1.5rem',
    borderRadius: 6,
    border: 'none',
    background: '#f38ba8',
    color: '#1e1e2e',
    fontWeight: 600,
    fontSize: '1rem',
    cursor: 'pointer',
    width: '100%',
  },
};