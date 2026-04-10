import { useEffect, useState } from 'react';
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
      <div className="page-center">
        <p className="text-error">{error}</p>
      </div>
    );
  }

  if (!game) {
    return (
      <div className="page-center">
        <div className="spinner" aria-label="spinner" />
      </div>
    );
  }

  if (isTerminal) {
    return (
      <div className="page-center">
        <div className="surface-card surface-card--wide">
          {outcome === 'won' && <h2 className="game-title game-title--won">You won!</h2>}
          {outcome === 'lost' && <h2 className="game-title game-title--lost">You lost.</h2>}
          {outcome === 'cancelled' && <h2 className="game-title game-title--cancelled">Game cancelled.</h2>}
          <p className="game-vs">{game.player1Username} vs {game.player2Username}</p>
          <button className="btn btn--primary-blue" style={{ marginTop: '0.5rem' }} onClick={onGameOver}>Back to Home</button>
        </div>
      </div>
    );
  }

  return (
    <div className="page-center">
      <div className="surface-card surface-card--wide">
        <h2 className="game-title">Match started!</h2>
        <p className="game-vs">
          {game.player1Username} vs {game.player2Username}
        </p>
        <p className="text-muted">Game is under construction. Stay tuned!</p>
        <button
          className="btn btn--danger"
          style={{ marginTop: '0.5rem' }}
          onClick={() => setShowForfeitConfirm(true)}
          disabled={forfeiting}
        >
          Forfeit
        </button>
      </div>

      {showForfeitConfirm && (
        <div className="modal-overlay" onClick={() => setShowForfeitConfirm(false)}>
          <div className="modal-box" onClick={e => e.stopPropagation()}>
            <p className="modal-title">Forfeit the match?</p>
            <p className="modal-sub">Your opponent will be declared the winner.</p>
            <div className="modal-actions">
              <button className="btn btn--ghost btn--sm" onClick={() => setShowForfeitConfirm(false)}>Stay</button>
              <button className="btn btn--danger btn--sm" onClick={handleForfeitConfirm} disabled={forfeiting}>
                {forfeiting ? '...' : 'Forfeit'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}