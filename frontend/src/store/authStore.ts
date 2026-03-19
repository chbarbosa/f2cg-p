import { create } from 'zustand';

interface AuthState {
  playerId: string | null;
  token: string | null;
  username: string | null;
  pendingEmail: string | null;
  login: (playerId: string, token: string, username: string) => void;
  logout: () => void;
  setPendingEmail: (email: string) => void;
  clearPendingEmail: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  playerId: null,
  token: null,
  username: null,
  pendingEmail: null,
  login: (playerId, token, username) => set({ playerId, token, username, pendingEmail: null }),
  logout: () => set({ playerId: null, token: null, username: null, pendingEmail: null }),
  setPendingEmail: (email) => set({ pendingEmail: email }),
  clearPendingEmail: () => set({ pendingEmail: null }),
}));