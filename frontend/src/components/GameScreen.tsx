import React, { useEffect, useState } from 'react';
import { getGame, sendHeartbeat, forfeitGame } from '../api/game';
import { useAuthStore } from '../store/authStore';
import type { GameResponse } from '../api/types';

interface Props {
  gamePublicId: string;
  onGameOver: () => void;
}

export function GameScreen({ gamePublicId, onGameOver }: Props) {
  const [game, setGame] = useState<GameResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showForfeitConfirm, setShowForfeitConfirm] = useState(false);
  const [forfeiting, setForfeiting] = useState(false);
  const playerId = useAuthStore(s => s.playerId);

  const isTerminal = game?.status === 'FINISHED' || game?.status === 'CANCELLED';

  // Initial load + first heartbeat
  useEffect(() => {
    sendHeartbeat(gamePublicId);
    getGame(gamePublicId)
      .then(setGame)
      .catch(() => setError('Failed to load game.'));
  }, [gamePublicId]);

  // Heartbeat every 15s while active
  useEffect(() => {
    if (isTerminal) return;
    const id = setInterval(() => sendHeartbeat(gamePublicId), 15_000);
    return () => clearInterval(id);
  }, [gamePublicId, isTerminal]);

  // Status poll every 30s while active
  useEffect(() => {
    if (isTerminal) return;
    const id = setInterval(() => {
      getGame(gamePublicId).then(setGame).catch(() => {});
    }, 30_000);
    return () => clearInterval(id);
  }, [gamePublicId, isTerminal]);

  const handleForfeitConfirm = async () => {
    setForfeiting(true);
    try {
      forfeitGame(gamePublicId);
      const updated = await getGame(gamePublicId);
      setGame(updated);
    } catch {
      setError('Failed to forfeit. Please try again.');
    } finally {
      setForfeiting(false);
      setShowForfeitConfirm(false);
    }
  };

  const outcome: 'won' | 'lost' | 'cancelled' | null = (() => {
    if (!game || !isTerminal) return null;
    if (game.status === 'CANCELLED') return 'cancelled';
    return game.winnerId === playerId ? 'won' : 'lost';
  })();

  if (error) {
    return (
      <div style={styles.center}>
        <p style={styles.errorText}>{error}</p>
      </div>
    );
  }

  if (!game) {
    return (
      <div style={styles.center}>
        <div style={styles.spinner} aria-label="spinner" />
      </div>
    );
  }

  if (isTerminal) {
    return (
      <div style={styles.center}>
        <div style={styles.card}>
          {outcome === 'won' && <h2 style={{ ...styles.title, color: '#a6e3a1' }}>You won!</h2>}
          {outcome === 'lost' && <h2 style={{ ...styles.title, color: '#f38ba8' }}>You lost.</h2>}
          {outcome === 'cancelled' && <h2 style={{ ...styles.title, color: '#fab387' }}>Game cancelled.</h2>}
          <p style={styles.vs}>{game.player1Username} vs {game.player2Username}</p>
          <button style={styles.homeBtn} onClick={onGameOver}>Back to Home</button>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.center}>
      <div style={styles.card}>
        <h2 style={styles.title}>Match started!</h2>
        <p style={styles.vs}>
          {game.player1Username} vs {game.player2Username}
        </p>
        <p style={styles.sub}>Game is under construction. Stay tuned!</p>
        <button
          style={styles.forfeitBtn}
          onClick={() => setShowForfeitConfirm(true)}
          disabled={forfeiting}
        >
          Forfeit
        </button>
      </div>

      {showForfeitConfirm && (
        <div style={styles.overlay} onClick={() => setShowForfeitConfirm(false)}>
          <div style={styles.modal} onClick={e => e.stopPropagation()}>
            <p style={styles.modalTitle}>Forfeit the match?</p>
            <p style={styles.modalSub}>Your opponent will be declared the winner.</p>
            <div style={styles.modalActions}>
              <button style={styles.stayBtn} onClick={() => setShowForfeitConfirm(false)}>Stay</button>
              <button style={styles.confirmBtn} onClick={handleForfeitConfirm} disabled={forfeiting}>
                {forfeiting ? '...' : 'Forfeit'}
              </button>
            </div>
          </div>
        </div>
      )}
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
    padding: '2.5rem 2rem',
    width: 380,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '1rem',
    textAlign: 'center',
  },
  title: { margin: 0, color: '#a6e3a1', fontSize: '1.5rem' },
  vs: { margin: 0, color: '#cdd6f4', fontSize: '1.1rem', fontWeight: 600 },
  sub: { margin: 0, color: '#a6adc8', fontSize: '0.9rem' },
  errorText: { color: '#f38ba8', fontSize: '1rem' },
  spinner: {
    width: 48,
    height: 48,
    borderRadius: '50%',
    border: '4px solid #313244',
    borderTopColor: '#89b4fa',
    animation: 'spin 1s linear infinite',
  },
  forfeitBtn: {
    marginTop: '0.5rem',
    padding: '0.4rem 1.2rem',
    borderRadius: 6,
    border: 'none',
    background: '#f38ba8',
    color: '#1e1e2e',
    fontWeight: 700,
    fontSize: '0.9rem',
    cursor: 'pointer',
  },
  homeBtn: {
    marginTop: '0.5rem',
    padding: '0.5rem 1.5rem',
    borderRadius: 6,
    border: 'none',
    background: '#89b4fa',
    color: '#1e1e2e',
    fontWeight: 600,
    fontSize: '0.95rem',
    cursor: 'pointer',
  },
  overlay: {
    position: 'fixed',
    inset: 0,
    background: 'rgba(0,0,0,0.6)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
  },
  modal: {
    background: '#1e1e2e',
    border: '1px solid #45475a',
    borderRadius: 10,
    padding: '1.5rem',
    minWidth: 300,
    boxShadow: '0 8px 32px rgba(0,0,0,0.5)',
  },
  modalTitle: { margin: '0 0 0.5rem', color: '#cdd6f4', fontWeight: 700, fontSize: '1.05rem' },
  modalSub: { margin: '0 0 1.25rem', color: '#a6adc8', fontSize: '0.9rem' },
  modalActions: { display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' },
  stayBtn: {
    padding: '0.3rem 0.8rem',
    borderRadius: 4,
    border: 'none',
    background: '#313244',
    color: '#cdd6f4',
    cursor: 'pointer',
    fontSize: '0.85rem',
  },
  confirmBtn: {
    padding: '0.3rem 0.8rem',
    borderRadius: 4,
    border: 'none',
    background: '#f38ba8',
    color: '#1e1e2e',
    cursor: 'pointer',
    fontSize: '0.85rem',
    fontWeight: 700,
  },
};