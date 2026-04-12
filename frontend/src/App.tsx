import { useEffect, useState } from 'react';
import { AuthForm } from './components/AuthForm';
import { ConfigScreen } from './components/ConfigScreen';
import { DeckBuilder } from './components/DeckBuilder';
import { DeckList } from './components/DeckList';
import { DeckSelector } from './components/DeckSelector';
import { GameScreen } from './components/GameScreen';
import { ProfileSetup } from './components/ProfileSetup';
import { QueueWaiting } from './components/QueueWaiting';
import { PerformanceScreen } from './components/PerformanceScreen';
import { useAuthStore } from './store/authStore';
import { useDeckStore } from './store/deckStore';
import { useQueueStore } from './store/queueStore';
import { BoardPreview } from './components/board/BoardPreview';
import { NavItem } from './components/ui';

type View = 'home' | 'deckList' | 'deckBuilder' | 'profileSetup' | 'deckSelector' | 'queueWaiting' | 'config' | 'performance' | 'game';

export default function App() {
  if (import.meta.env.DEV && new URLSearchParams(window.location.search).has('devBoard')) {
    return <BoardPreview />;
  }

  const { username, playerId, nickname, logout } = useAuthStore();
  const { decks, fetchDecks } = useDeckStore();
  const { join, clearEntry } = useQueueStore();
  const [view, setView] = useState<View>('home');
  const [gamePublicId, setGamePublicId] = useState<string | null>(null);

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

  const handleMatchFound = (publicId: string) => {
    clearEntry();
    setGamePublicId(publicId);
    setView('game');
  };

  const handleReady = async (deckId: string) => {
    await join(deckId);
    setView('queueWaiting');
  };

  const hasPlayableDeck = decks.some(d => d.status === 'PLAYABLE');
  const isPlayBlocked = !!nickname && !hasPlayableDeck;

  if (username) {
    if (view === 'deckList') {
      return (
        <div className="page-bg">
          <DeckList onBack={handleHome} onNew={handleEditDeck} onEdit={handleEditDeck} />
        </div>
      );
    }

    if (view === 'deckBuilder') {
      return (
        <div className="page-bg">
          <DeckBuilder onCancel={handleListDecks} onSaved={handleListDecks} />
        </div>
      );
    }

    if (view === 'profileSetup') {
      return (
        <div className="page-center">
          <ProfileSetup onDone={() => setView('deckSelector')} />
        </div>
      );
    }

    if (view === 'config') {
      return <ConfigScreen onBack={handleHome} />;
    }

    if (view === 'performance') {
      return (
        <div className="page-bg">
          <PerformanceScreen onBack={handleHome} />
        </div>
      );
    }

    if (view === 'deckSelector') {
      return (
        <div className="page-bg">
          <DeckSelector
            onReady={handleReady}
            onBack={handleHome}
          />
        </div>
      );
    }

    if (view === 'queueWaiting') {
      return <QueueWaiting
        onCancelled={() => { clearEntry(); handleHome(); }}
        onMatchFound={handleMatchFound}
      />;
    }

    if (view === 'game' && gamePublicId) {
      return <GameScreen
        gamePublicId={gamePublicId}
        onGameOver={() => { setGamePublicId(null); setView('home'); }}
      />;
    }

    return (
      <div className="page-center">
        <div className="surface-card surface-card--narrow">
          <h2 className="app-welcome">Welcome, {username}!</h2>
          <p className="app-sub">Player ID: <code className="app-code-chip">{playerId}</code></p>
          <div className="app-play-wrapper" title={isPlayBlocked ? 'You need at least one playable deck to battle' : undefined}>
            <NavItem
              variant="secondary"
              fullWidth
              disabled={isPlayBlocked}
              onClick={() => setView(nickname ? 'deckSelector' : 'profileSetup')}
            >
              Play
            </NavItem>
          </div>
          <div className="app-play-wrapper" title={!hasPlayableDeck ? 'You need at least one playable deck to battle' : undefined}>
            <NavItem variant="secondary" fullWidth disabled>Pratique</NavItem>
          </div>
          <NavItem variant="secondary" fullWidth onClick={handleListDecks}>My Decks</NavItem>
          <NavItem variant="tertiary" fullWidth disabled>Store</NavItem>
          <NavItem variant="secondary" fullWidth onClick={() => setView('performance')}>Performance</NavItem>
          <NavItem variant="secondary" fullWidth onClick={() => setView('config')}>Config</NavItem>
          <NavItem variant="secondary" fullWidth onClick={handleLogout}>Logout</NavItem>
        </div>
      </div>
    );
  }

  return (
    <div className="page-center">
      <AuthForm />
    </div>
  );
}