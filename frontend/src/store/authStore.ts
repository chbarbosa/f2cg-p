import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  playerId: string | null;
  token: string | null;
  username: string | null;
  nickname: string | null;
  country: string | null;
  pendingEmail: string | null;
  sessionExpired: boolean;
  login: (playerId: string, token: string, username: string, nickname: string | null, country: string | null) => void;
  setProfile: (nickname: string | null, country: string | null) => void;
  logout: () => void;
  expireSession: () => void;
  setPendingEmail: (email: string) => void;
  clearPendingEmail: () => void;
}

export const useAuthStore = create<AuthState>()(persist((set) => ({
  playerId: null,
  token: null,
  username: null,
  nickname: null,
  country: null,
  pendingEmail: null,
  sessionExpired: false,
  login: (playerId, token, username, nickname, country) =>
    set({ playerId, token, username, nickname, country, pendingEmail: null, sessionExpired: false }),
  setProfile: (nickname, country) => set({ nickname, country }),
  logout: () => set({ playerId: null, token: null, username: null, nickname: null, country: null, pendingEmail: null, sessionExpired: false }),
  expireSession: () => set({ playerId: null, token: null, username: null, nickname: null, country: null, pendingEmail: null, sessionExpired: true }),
  setPendingEmail: (email) => set({ pendingEmail: email }),
  clearPendingEmail: () => set({ pendingEmail: null }),
}), { name: 'auth' }));