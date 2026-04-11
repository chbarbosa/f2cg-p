export type UnitClass = 'WARRIOR' | 'MAGE' | 'PALADIN' | 'CLERIC' | 'ASSASSIN' | 'WARLOCK' | 'ARCHER';
export type GamePhase = 'SUMMONING' | 'ACTION' | 'DRAW' | 'CHECK_VICTORY';

export interface UnitCard {
  id: string;
  name: string;
  type: 'UNIT';
  unitClass: UnitClass;
  manaCost: number;
  attack: number;
  defense: number;
}

export interface BuffCard {
  id: string;
  name: string;
  type: 'BUFF';
  manaCost: number;
}

export interface DebuffCard {
  id: string;
  name: string;
  type: 'DEBUFF';
  manaCost: number;
}

export type Card = UnitCard | BuffCard | DebuffCard;

export interface FieldUnit {
  card: UnitCard;
  currentAttack: number;
  currentDefense: number;
  activeBuff: { name: string } | null;
  activeDebuff: { name: string } | null;
  hasActed: boolean;
}

export interface BoardPlayer {
  playerId: string;
  username: string;
  hand: Card[];
  field: FieldUnit[];
  deckSize: number;
  graveyard: Card[];
}

export interface BoardOpponent {
  playerId: string;
  username: string;
  handSize: number;
  field: FieldUnit[];
  deckSize: number;
  graveyard: Card[];
}

export interface GameBoardProps {
  gameId: string;
  currentPlayerId: string;
  currentMana: number;
  turnNumber: number;
  phase: GamePhase;
  activePlayerId: string;
  player: BoardPlayer;
  opponent: BoardOpponent;
  selectedCardId: string | null;
  onCardClick: (cardId: string) => void;
}