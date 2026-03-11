import { create } from 'zustand';

interface AuthState {
  playerId: string | null;
  token: string | null;
  username: string | null;
  login: (playerId: string, token: string, username: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  playerId: null,
  token: null,
  username: null,
  login: (playerId, token, username) => set({ playerId, token, username }),
  logout: () => set({ playerId: null, token: null, username: null }),
}));