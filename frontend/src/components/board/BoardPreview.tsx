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
      },
      {
        id: 'card-mage-1',
        name: 'Arcane Scholar',
        type: 'UNIT',
        unitClass: 'MAGE',
        manaCost: 4,
        attack: 6,
        defense: 3,
      },
      {
        id: 'card-buff-1',
        name: 'Battle Cry',
        type: 'BUFF',
        manaCost: 1,
      },
      {
        id: 'card-debuff-1',
        name: 'Weaken',
        type: 'DEBUFF',
        manaCost: 2,
      },
      {
        id: 'card-expensive-1',
        name: 'Dragon Lord',
        type: 'UNIT',
        unitClass: 'WARLOCK',
        manaCost: 5,
        attack: 10,
        defense: 8,
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
        },
        currentAttack: 7,
        currentDefense: 7,
        activeBuff: { name: 'Battle Cry' },
        activeDebuff: null,
        hasActed: false,
      },
    ],
    deckSize: 15,
    graveyard: [
      { id: 'grave-1', name: 'Fallen Scout', type: 'UNIT', unitClass: 'WARRIOR', manaCost: 1, attack: 2, defense: 2 },
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
        },
        currentAttack: 4,
        currentDefense: 2,
        activeBuff: null,
        activeDebuff: { name: 'Weaken' },
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