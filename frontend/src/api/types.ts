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

export type QueueStatus = 'WAITING' | 'MATCHED' | 'CANCELLED';

export type PlayerRank = 'ELITE' | 'ADVANCED' | 'INTERMEDIATE' | 'ROOKIE' | 'PENDING';
export type SeasonPhase = 'FREE' | 'RANKED';

export interface SeasonSummary {
  id: string;
  year: number;
  seasonNumber: number;
  name: string | null;
  startDate: string;
  endDate: string;
  phase2StartDate: string;
}

export interface PerformanceResponse {
  season: SeasonSummary;
  currentPhase: SeasonPhase | null;
  rank: PlayerRank;
  highestRank: PlayerRank;
  totalMatches: number;
  victories: number;
  defeats: number;
  matchesThisWeek: number;
}

export interface QueueEntryResponse {
  id: string;
  playerId: string;
  deckId: string;
  status: QueueStatus;
  joinedAt: string;
}