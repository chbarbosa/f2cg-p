import { useState } from 'react';
import { GameBoard } from './GameBoard';
import type { GameBoardProps } from '../../api/gameTypes';

const MOCK_PROPS: GameBoardProps = {
  gameId: 'preview-game',
  currentPlayerId: 'player-1',
  currentMana: 3,
  turnNumber: 2,
  phase: 'ACTION',
  activePlayerId: 'player-1',
  player: {
    playerId: 'player-1',
    username: 'You',
    hand: [
      {
        id: 'card-warrior-1',
        name: 'Iron Knight',
        type: 'UNIT',
        unitClass: 'WARRIOR',
        manaCost: 2,
        attack: 4,
        defense: 6,
        abilities: [
          {
            id: 'ability-1',
            name: 'Shield Bash',
            type: 'ACTIVE',
            effect: { type: 'DEF_REDUCTION', value: 2 },
            description: 'Reduces enemy DEF by 2.',
          },
          {
            id: 'ability-2',
            name: 'Iron Will',
            type: 'PASSIVE',
            effect: { type: 'DEF_BOOST', value: 1 },
            description: 'Gains +1 DEF at the start of each turn.',
          },
        ],
      },
      {
        id: 'card-mage-1',
        name: 'Arcane Scholar',
        type: 'UNIT',
        unitClass: 'MAGE',
        manaCost: 4,
        attack: 6,
        defense: 3,
        abilities: [],
      },
      {
        id: 'card-buff-1',
        name: 'Battle Cry',
        type: 'BUFF',
        manaCost: 1,
        effect: { type: 'ATK_BOOST', value: 2 },
      },
      {
        id: 'card-debuff-1',
        name: 'Weaken',
        type: 'DEBUFF',
        manaCost: 2,
        effect: { type: 'ATK_REDUCTION', value: 2 },
      },
      {
        id: 'card-expensive-1',
        name: 'Dragon Lord',
        type: 'UNIT',
        unitClass: 'WARLOCK',
        manaCost: 5,
        attack: 10,
        defense: 8,
        abilities: [],
      },
    ],
    field: [
      {
        card: {
          id: 'field-warrior-1',
          name: 'Stone Guard',
          type: 'UNIT',
          unitClass: 'WARRIOR',
          manaCost: 3,
          attack: 5,
          defense: 7,
          abilities: [],
        },
        currentAttack: 7,
        currentDefense: 7,
        activeBuff: { name: 'Battle Cry', effect: { type: 'ATK_BOOST', value: 2 } },
        activeDebuff: null,
        hasActed: false,
      },
      {
        card: {
          id: 'field-paladin-1',
          name: 'Tired Paladin',
          type: 'UNIT',
          unitClass: 'PALADIN',
          manaCost: 2,
          attack: 3,
          defense: 4,
          abilities: [],
        },
        currentAttack: 3,
        currentDefense: 4,
        activeBuff: null,
        activeDebuff: null,
        hasActed: true,
      },
    ],
    deckSize: 15,
    graveyard: [
      {
        id: 'grave-1',
        name: 'Fallen Scout',
        type: 'UNIT',
        unitClass: 'WARRIOR',
        manaCost: 1,
        attack: 2,
        defense: 2,
        abilities: [],
      },
    ],
  },
  opponent: {
    playerId: 'player-2',
    username: 'Opponent',
    handSize: 5,
    field: [
      {
        card: {
          id: 'opp-field-1',
          name: 'Dark Mage',
          type: 'UNIT',
          unitClass: 'MAGE',
          manaCost: 3,
          attack: 6,
          defense: 3,
          abilities: [],
        },
        currentAttack: 4,
        currentDefense: 2,
        activeBuff: null,
        activeDebuff: { name: 'Weaken', effect: { type: 'ATK_REDUCTION', value: 2 } },
        hasActed: false,
      },
    ],
    deckSize: 12,
    graveyard: [],
  },
  selectedCardId: 'card-warrior-1',
  onCardClick: () => {},
};

export function BoardPreview() {
  const [lastClicked, setLastClicked] = useState<string | null>(null);
  const [selectedCardId, setSelectedCardId] = useState<string | null>(MOCK_PROPS.selectedCardId);

  const handleCardClick = (cardId: string) => {
    setLastClicked(cardId);
    setSelectedCardId(cardId);
  };

  return (
    <>
      <GameBoard
        {...MOCK_PROPS}
        selectedCardId={selectedCardId}
        onCardClick={handleCardClick}
      />
      <span
        data-testid="last-clicked"
        style={{ display: 'none' }}
      >
        {lastClicked ?? ''}
      </span>
    </>
  );
}