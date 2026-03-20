import { get, post, del } from './http';
import type { QueueEntryResponse } from './types';

export const joinQueue = (deckId: string): Promise<QueueEntryResponse> =>
  post<QueueEntryResponse>('/api/queue', { deckId });

export const cancelQueue = (): Promise<void> =>
  del('/api/queue');

export const getQueueStatus = (): Promise<QueueEntryResponse> =>
  get<QueueEntryResponse>('/api/queue/status');