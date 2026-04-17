import { get } from './http';
import { useAuthStore } from '../store/authStore';
import type { GameResponse } from './types';
import type { PlayerGameStateView } from './gameTypes';

export function getGame(publicId: string): Promise<GameResponse> {
  return get<GameResponse>(`/api/game/${publicId}`);
}

export function getBoardState(publicId: string): Promise<PlayerGameStateView> {
  return get<PlayerGameStateView>(`/api/game/${publicId}/state`);
}

export function sendHeartbeat(publicId: string): void {
  const token = useAuthStore.getState().token;
  fetch(`/api/game/${publicId}/heartbeat`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token ?? ''}` },
  }).catch(() => {});
}

export function forfeitGame(publicId: string): void {
  const token = useAuthStore.getState().token;
  fetch(`/api/game/${publicId}/forfeit`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token ?? ''}` },
  }).catch(() => {});
}