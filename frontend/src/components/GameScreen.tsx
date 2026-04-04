import React, { useEffect, useState } from 'react';
import { getGame } from '../api/game';
import type { GameResponse } from '../api/types';

interface Props {
  gamePublicId: string;
}

export function GameScreen({ gamePublicId }: Props) {
  const [game, setGame] = useState<GameResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getGame(gamePublicId)
      .then(setGame)
      .catch(() => setError('Failed to load game.'));
  }, [gamePublicId]);

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

  return (
    <div style={styles.center}>
      <div style={styles.card}>
        <h2 style={styles.title}>Match started!</h2>
        <p style={styles.vs}>
          {game.player1Username} vs {game.player2Username}
        </p>
        <p style={styles.sub}>Game is under construction. Stay tuned!</p>
      </div>
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
};