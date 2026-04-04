import { get } from './http';
import type { GameResponse } from './types';

export function getGame(publicId: string): Promise<GameResponse> {
  return get<GameResponse>(`/api/game/${publicId}`);
}