import { useEffect, useRef, useState, useCallback, useMemo } from 'react';
import { getBoardState, sendHeartbeat } from '../api/game';
import { useAuthStore } from '../store/authStore';
import { useGameStore } from '../store/gameStore';
import { useSSE } from './useSSE';
import type { PlayerGameStateView } from '../api/gameTypes';

const HEARTBEAT_INTERVAL_MS = 15_000;

export function useGame(gamePublicId: string) {
  const token = useAuthStore.getState().token;
  const { gameState, isConnected, isLoading, error, setGameState, setConnected, setLoading, setError, resetGame } =
    useGameStore();
  const [selectedCardId, setSelectedCardId] = useState<string | null>(null);
  const heartbeatRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const sseUrl = token
    ? `/api/game/${gamePublicId}/stream?token=${encodeURIComponent(token)}`
    : null;

  const eventHandlers = useMemo(
    () => ({
      GAME_STATE_UPDATE: (data: unknown) => setGameState(data as PlayerGameStateView),
    }),
    [setGameState]
  );

  const { isReconnecting } = useSSE(sseUrl, eventHandlers, {
    onOpen: () => setConnected(true),
    onError: () => {
      setConnected(false);
      setError('Connection lost. Please refresh the page.');
    },
  });

  useEffect(() => {
    setLoading(true);
    getBoardState(gamePublicId)
      .then((state) => setGameState(state))
      .catch(() => setError('Failed to load game state.'))
      .finally(() => setLoading(false));

    heartbeatRef.current = setInterval(() => sendHeartbeat(gamePublicId), HEARTBEAT_INTERVAL_MS);
    sendHeartbeat(gamePublicId);

    return () => {
      if (heartbeatRef.current) clearInterval(heartbeatRef.current);
      resetGame();
    };
  }, [gamePublicId]);

  const selectCard = useCallback(
    (cardId: string) => {
      setSelectedCardId((prev) => (prev === cardId ? null : cardId));
    },
    []
  );

  return { gameState, isConnected, isReconnecting, isLoading, error, selectedCardId, selectCard };
}