import { get, post, put, del } from './http';
import type {
  CreateDeckRequest,
  DeckResponse,
  DeckWithCardsResponse,
  UpdateDeckRequest,
} from './types';

export const listDecks = (): Promise<DeckResponse[]> =>
  get<DeckResponse[]>('/api/decks');

export const getDeck = (id: string): Promise<DeckWithCardsResponse> =>
  get<DeckWithCardsResponse>(`/api/decks/${id}`);

export const createDeck = (req: CreateDeckRequest): Promise<DeckResponse> =>
  post<DeckResponse>('/api/decks', req);

export const updateDeck = (id: string, req: UpdateDeckRequest): Promise<DeckResponse> =>
  put<DeckResponse>(`/api/decks/${id}`, req);

export const deleteDeck = (id: string): Promise<void> =>
  del(`/api/decks/${id}`);