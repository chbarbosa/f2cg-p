import { useEffect, useState } from 'react';
import { getGame, sendHeartbeat, forfeitGame, getBoardState } from '../api/game';
import { useAuthStore } from '../store/authStore';
import type { GameResponse } from '../api/types';
import type { PlayerGameStateView } from '../api/gameTypes';
import { GameBoard } from './board/GameBoard';
import { PrimaryButton, SecondaryButton, DangerButton } from './ui';

interface Props {
  gamePublicId: string;
  onGameOver: () => void;
}

export function GameScreen({ gamePublicId, onGameOver }: Props) {
  const [game, setGame] = useState<GameResponse | null>(null);
  const [boardState, setBoardState] = useState<PlayerGameStateView | null>(null);
  const [selectedCardId, setSelectedCardId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showForfeitConfirm, setShowForfeitConfirm] = useState(false);
  const [forfeiting, setForfeiting] = useState(false);
  const playerId = useAuthStore(s => s.playerId);

  const isTerminal = game?.status === 'FINISHED' || game?.status === 'CANCELLED';
  const isInProgress = game?.status === 'IN_PROGRESS';

  // Initial load + first heartbeat
  useEffect(() => {
    sendHeartbeat(gamePublicId);
    getGame(gamePublicId)
      .then(setGame)
      .catch(() => setError('Failed to load game.'));
  }, [gamePublicId]);

  // Fetch board state when game transitions to IN_PROGRESS
  useEffect(() => {
    if (!isInProgress || boardState) return;
    getBoardState(gamePublicId)
      .then(setBoardState)
      .catch(() => {});
  }, [gamePublicId, isInProgress, boardState]);

  // Heartbeat every 15s while active
  useEffect(() => {
    if (isTerminal) return;
    const id = setInterval(() => sendHeartbeat(gamePublicId), 15_000);
    return () => clearInterval(id);
  }, [gamePublicId, isTerminal]);

  // Poll game status + board state every 30s while active
  useEffect(() => {
    if (isTerminal) return;
    const id = setInterval(() => {
      getGame(gamePublicId).then(setGame).catch(() => {});
      getBoardState(gamePublicId).then(setBoardState).catch(() => {});
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
          <div style={{ marginTop: '0.5rem' }}>
            <PrimaryButton onClick={onGameOver}>Back to Home</PrimaryButton>
          </div>
        </div>
      </div>
    );
  }

  if (isInProgress && boardState) {
    return (
      <>
        <GameBoard
          gameId={boardState.gameId}
          currentPlayerId={boardState.me.playerId}
          currentMana={boardState.currentMana}
          turnNumber={boardState.turnNumber}
          phase={boardState.phase}
          activePlayerId={boardState.activePlayerId}
          player={{
            playerId: boardState.me.playerId,
            username: boardState.me.username,
            hand: boardState.me.hand,
            field: boardState.me.field,
            deckSize: boardState.me.stackSize,
            graveyard: boardState.me.graveyard,
          }}
          opponent={{
            playerId: boardState.opponent.playerId,
            username: boardState.opponent.username,
            handSize: boardState.opponent.handSize,
            field: boardState.opponent.field,
            deckSize: boardState.opponent.stackSize,
            graveyard: boardState.opponent.graveyard,
          }}
          selectedCardId={selectedCardId}
          onCardClick={setSelectedCardId}
        />
        <div style={{ position: 'fixed', bottom: '1rem', left: '1rem' }}>
          <DangerButton onClick={() => setShowForfeitConfirm(true)} disabled={forfeiting}>
            Forfeit
          </DangerButton>
        </div>

        {showForfeitConfirm && (
          <div className="modal-overlay" onClick={() => setShowForfeitConfirm(false)}>
            <div className="modal-box" onClick={e => e.stopPropagation()}>
              <p className="modal-title">Forfeit the match?</p>
              <p className="modal-sub">Your opponent will be declared the winner.</p>
              <div className="modal-actions">
                <SecondaryButton onClick={() => setShowForfeitConfirm(false)}>Stay</SecondaryButton>
                <DangerButton onClick={handleForfeitConfirm} disabled={forfeiting}>
                  {forfeiting ? '...' : 'Forfeit'}
                </DangerButton>
              </div>
            </div>
          </div>
        )}
      </>
    );
  }

  return (
    <div className="page-center">
      <div className="surface-card surface-card--wide">
        <h2 className="game-title">Match started!</h2>
        <p className="game-vs">
          {game.player1Username} vs {game.player2Username}
        </p>
        <p className="text-muted">Waiting for match to begin...</p>
        <div style={{ marginTop: '0.5rem' }}>
          <DangerButton
            onClick={() => setShowForfeitConfirm(true)}
            disabled={forfeiting}
          >
            Forfeit
          </DangerButton>
        </div>
      </div>

      {showForfeitConfirm && (
        <div className="modal-overlay" onClick={() => setShowForfeitConfirm(false)}>
          <div className="modal-box" onClick={e => e.stopPropagation()}>
            <p className="modal-title">Forfeit the match?</p>
            <p className="modal-sub">Your opponent will be declared the winner.</p>
            <div className="modal-actions">
              <SecondaryButton onClick={() => setShowForfeitConfirm(false)}>Stay</SecondaryButton>
              <DangerButton onClick={handleForfeitConfirm} disabled={forfeiting}>
                {forfeiting ? '...' : 'Forfeit'}
              </DangerButton>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}