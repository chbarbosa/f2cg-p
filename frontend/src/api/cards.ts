import { get } from './http';
import type { CardResponse, DeckTheme } from './types';

export const getCardsByTheme = (theme: DeckTheme): Promise<CardResponse[]> =>
  get<CardResponse[]>(`/api/cards?theme=${theme}`);