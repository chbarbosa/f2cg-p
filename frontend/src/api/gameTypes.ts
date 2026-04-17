export type UnitClass = 'WARRIOR' | 'MAGE' | 'PALADIN' | 'CLERIC' | 'ASSASSIN' | 'WARLOCK' | 'ARCHER';
export type GamePhase = 'SUMMONING' | 'ACTION' | 'DRAW' | 'CHECK_VICTORY';
export type AbilityType = 'PASSIVE' | 'ACTIVE';
export type EffectType = 'ATK_BOOST' | 'DEF_BOOST' | 'ATK_REDUCTION' | 'DEF_REDUCTION';

export interface Effect {
  type: EffectType;
  value: number;
}

export interface Ability {
  id: string;
  name: string;
  type: AbilityType;
  effect: Effect;
  description: string;
}

export interface UnitCard {
  id: string;
  name: string;
  type: 'UNIT';
  unitClass: UnitClass;
  manaCost: number;
  attack: number;
  defense: number;
  abilities: Ability[];
}

export interface BuffCard {
  id: string;
  name: string;
  type: 'BUFF';
  manaCost: number;
  effect: Effect;
}

export interface DebuffCard {
  id: string;
  name: string;
  type: 'DEBUFF';
  manaCost: number;
  effect: Effect;
}

export type Card = UnitCard | BuffCard | DebuffCard;

export interface FieldUnit {
  card: UnitCard;
  currentAttack: number;
  currentDefense: number;
  activeBuff: { name: string; effect: Effect } | null;
  activeDebuff: { name: string; effect: Effect } | null;
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

export interface PlayerMeView {
  playerId: string;
  username: string;
  hand: Card[];
  stackSize: number;
  field: FieldUnit[];
  graveyard: Card[];
  summoningConfirmed: boolean;
}

export interface PlayerOpponentView {
  playerId: string;
  username: string;
  handSize: number;
  stackSize: number;
  field: FieldUnit[];
  graveyard: Card[];
  summoningConfirmed: boolean;
}

export interface PlayerGameStateView {
  gameId: string;
  turnNumber: number;
  currentMana: number;
  phase: GamePhase;
  activePlayerId: string;
  me: PlayerMeView;
  opponent: PlayerOpponentView;
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