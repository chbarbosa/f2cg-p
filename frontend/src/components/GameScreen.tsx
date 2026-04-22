import { useEffect, useState } from 'react';
import { getGame, forfeitGame } from '../api/game';
import { useAuthStore } from '../store/authStore';
import { useGame } from '../hooks/useGame';
import type { GameResponse } from '../api/types';
import { GameBoard } from './board/GameBoard';
import { PrimaryButton, SecondaryButton, DangerButton, TertiaryButton } from './ui';

interface Props {
  gamePublicId: string;
  onGameOver: () => void;
}

export function GameScreen({ gamePublicId, onGameOver }: Props) {
  const [game, setGame] = useState<GameResponse | null>(null);
  const [showForfeitConfirm, setShowForfeitConfirm] = useState(false);
  const [forfeiting, setForfeiting] = useState(false);
  const playerId = useAuthStore(s => s.playerId);

  const { gameState, isConnected, isReconnecting, isLoading, error, selectedCardId, selectCard } =
    useGame(gamePublicId);

  const isTerminal = game?.status === 'FINISHED' || game?.status === 'CANCELLED';

  // Poll game lifecycle (terminal state detection)
  useEffect(() => {
    getGame(gamePublicId).then(setGame).catch(() => {});
    if (isTerminal) return;
    const id = setInterval(() => getGame(gamePublicId).then(setGame).catch(() => {}), 30_000);
    return () => clearInterval(id);
  }, [gamePublicId, isTerminal]);

  const handleForfeitConfirm = async () => {
    setForfeiting(true);
    try {
      forfeitGame(gamePublicId);
      const updated = await getGame(gamePublicId);
      setGame(updated);
    } catch {
      // ignore
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

  if (isTerminal) {
    return (
      <div className="page-center">
        <div className="surface-card surface-card--wide">
          {outcome === 'won' && <h2 className="game-title game-title--won">You won!</h2>}
          {outcome === 'lost' && <h2 className="game-title game-title--lost">You lost.</h2>}
          {outcome === 'cancelled' && <h2 className="game-title game-title--cancelled">Game cancelled.</h2>}
          {game && <p className="game-vs">{game.player1Username} vs {game.player2Username}</p>}
          <div style={{ marginTop: '0.5rem' }}>
            <PrimaryButton onClick={onGameOver}>Back to Home</PrimaryButton>
          </div>
        </div>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="page-center">
        <div className="spinner" aria-label="spinner" />
        <p className="text-muted" style={{ marginTop: '1rem' }}>Loading game...</p>
      </div>
    );
  }

  if (error && !gameState) {
    return (
      <div className="page-center">
        <div className="surface-card surface-card--narrow">
          <p className="text-error">Something went wrong. Please try again.</p>
          <div style={{ marginTop: '0.5rem' }}>
            <TertiaryButton onClick={onGameOver}>Back to Home</TertiaryButton>
          </div>
        </div>
      </div>
    );
  }

  if (gameState) {
    return (
      <>
        <GameBoard
          gameId={gameState.gameId}
          currentPlayerId={gameState.me.playerId}
          currentMana={gameState.currentMana}
          turnNumber={gameState.turnNumber}
          phase={gameState.phase}
          activePlayerId={gameState.activePlayerId}
          player={{
            playerId: gameState.me.playerId,
            username: gameState.me.username,
            hand: gameState.me.hand,
            field: gameState.me.field,
            deckSize: gameState.me.stackSize,
            graveyard: gameState.me.graveyard,
          }}
          opponent={{
            playerId: gameState.opponent.playerId,
            username: gameState.opponent.username,
            handSize: gameState.opponent.handSize,
            field: gameState.opponent.field,
            deckSize: gameState.opponent.stackSize,
            graveyard: gameState.opponent.graveyard,
          }}
          selectedCardId={selectedCardId}
          onCardClick={selectCard}
        />

        <ConnectionStatus isConnected={isConnected} isReconnecting={isReconnecting} />

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
      <div className="spinner" aria-label="spinner" />
      <p className="text-muted" style={{ marginTop: '1rem' }}>Loading game...</p>
    </div>
  );
}

interface ConnectionStatusProps {
  isConnected: boolean;
  isReconnecting: boolean;
}

function ConnectionStatus({ isConnected, isReconnecting }: ConnectionStatusProps) {
  let color: string;
  let label: string;

  if (isConnected) {
    color = '#4ade80';
    label = 'Connected';
  } else if (isReconnecting) {
    color = '#facc15';
    label = 'Reconnecting...';
  } else {
    color = '#f87171';
    label = 'Connection lost';
  }

  return (
    <div style={{
      position: 'fixed',
      bottom: '1rem',
      right: '1rem',
      display: 'flex',
      alignItems: 'center',
      gap: '0.4rem',
      background: 'rgba(0,0,0,0.6)',
      padding: '0.3rem 0.6rem',
      borderRadius: '0.4rem',
      fontSize: '0.75rem',
      color: '#e5e7eb',
      zIndex: 100,
    }}>
      <span style={{
        width: '0.5rem',
        height: '0.5rem',
        borderRadius: '50%',
        background: color,
        display: 'inline-block',
      }} />
      {label}
    </div>
  );
}