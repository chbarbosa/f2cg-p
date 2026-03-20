import { putVoid } from './http';

export const updateProfile = (nickname: string | null, country: string | null): Promise<void> =>
  putVoid('/api/player/profile', { nickname, country });