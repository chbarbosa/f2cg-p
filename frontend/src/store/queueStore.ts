import { create } from 'zustand';
import { joinQueue, cancelQueue, getQueueStatus } from '../api/queue';
import type { QueueEntryResponse } from '../api/types';

interface QueueState {
  entry: QueueEntryResponse | null;
  loading: boolean;
  error: string | null;

  join: (deckId: string) => Promise<void>;
  cancel: () => Promise<void>;
  fetchStatus: () => Promise<void>;
  clearEntry: () => void;
}

export const useQueueStore = create<QueueState>((set) => ({
  entry: null,
  loading: false,
  error: null,

  join: async (deckId: string) => {
    set({ loading: true, error: null });
    try {
      const entry = await joinQueue(deckId);
      set({ entry, loading: false });
    } catch (e) {
      set({ error: (e as Error).message, loading: false });
      throw e;
    }
  },

  cancel: async () => {
    set({ loading: true, error: null });
    try {
      await cancelQueue();
      set({ entry: null, loading: false });
    } catch (e) {
      set({ error: (e as Error).message, loading: false });
      throw e;
    }
  },

  fetchStatus: async () => {
    set({ loading: true, error: null });
    try {
      const entry = await getQueueStatus();
      set({ entry, loading: false });
    } catch {
      set({ entry: null, loading: false });
    }
  },

  clearEntry: () => set({ entry: null, error: null }),
}));