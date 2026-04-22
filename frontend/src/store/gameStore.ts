import { create } from 'zustand';
import type { PlayerGameStateView } from '../api/gameTypes';

interface GameStore {
  gameId: string | null;
  gameState: PlayerGameStateView | null;
  isConnected: boolean;
  isLoading: boolean;
  error: string | null;
  setGameState: (state: PlayerGameStateView) => void;
  setConnected: (connected: boolean) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  resetGame: () => void;
}

export const useGameStore = create<GameStore>((set) => ({
  gameId: null,
  gameState: null,
  isConnected: false,
  isLoading: false,
  error: null,
  setGameState: (state) => set({ gameState: state, gameId: state.gameId }),
  setConnected: (connected) => set({ isConnected: connected }),
  setLoading: (loading) => set({ isLoading: loading }),
  setError: (error) => set({ error }),
  resetGame: () => set({ gameId: null, gameState: null, isConnected: false, isLoading: false, error: null }),
}));