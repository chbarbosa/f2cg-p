export type DeckTheme = 'WARRIOR' | 'MAGE' | 'CLERIC';
export type DeckStatus = 'DRAFT' | 'PLAYABLE';

export interface CardResponse {
  id: string;
  name: string;
  manaCost: number;
  cardType: string;
  theme: DeckTheme;
  unitClass: string | null;
  attack: number | null;
  defense: number | null;
  effectType: string | null;
  effectValue: number | null;
}

export interface DeckResponse {
  id: string;
  playerId: string;
  name: string;
  theme: DeckTheme;
  cardIds: string[];
  status: DeckStatus;
  createdAt: string;
  updatedAt: string;
}

export interface DeckWithCardsResponse {
  deck: DeckResponse;
  cards: CardResponse[];
}

export interface CreateDeckRequest {
  name: string;
  theme: DeckTheme;
  cardIds: string[];
}

export interface UpdateDeckRequest {
  name: string;
  theme: DeckTheme;
  cardIds: string[];
}