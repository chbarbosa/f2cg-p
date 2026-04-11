import { Card } from './Card';
import type { FieldUnit, GameBoardProps } from '../../api/gameTypes';
import './GameBoard.css';

function CardPile({ count, variant }: { count: number; variant: 'deck' | 'graveyard' }) {
  return (
    <div className={`card-pile card-pile--${variant}`}>
      <div className="card-pile__stack">
        <div className="card-pile__layer card-pile__layer--3" />
        <div className="card-pile__layer card-pile__layer--2" />
        <div className="card-pile__layer card-pile__layer--1">
          <span className="card-pile__count">{count}</span>
        </div>
      </div>
      <span className="card-pile__label">{variant === 'deck' ? 'DECK' : 'YARD'}</span>
    </div>
  );
}

function FieldUnitCard({
  unit,
  selected,
  onClick,
}: {
  unit: FieldUnit;
  selected: boolean;
  onClick: () => void;
}) {
  return (
    <Card
      id={unit.card.id}
      name={unit.card.name}
      type={unit.card.type}
      unitClass={unit.card.unitClass}
      attack={unit.currentAttack}
      defense={unit.currentDefense}
      manaCost={unit.card.manaCost}
      faceDown={false}
      selected={selected}
      playable={true}
      activeBuff={unit.activeBuff ?? undefined}
      activeDebuff={unit.activeDebuff ?? undefined}
      onClick={onClick}
    />
  );
}

export function GameBoard({
  currentPlayerId,
  currentMana,
  turnNumber,
  phase,
  activePlayerId,
  player,
  opponent,
  selectedCardId,
  onCardClick,
}: GameBoardProps) {
  const isPlayerActive = activePlayerId === currentPlayerId;

  return (
    <div className="game-board">
      {/* 1. OPPONENT INFO BAR */}
      <div className="board__opponent-info">
        <span className="board__info-username">{opponent.username}</span>
      </div>

      {/* 2. OPPONENT HAND */}
      <div className="board__opponent-hand" data-testid="opponent-hand">
        <div className="board__hand-zone__spacer" />
        <div className="board__hand-zone__center">
          <span className="board__hand-label">{opponent.handSize} cards in hand</span>
          <div className="board__hand-row">
            {Array.from({ length: opponent.handSize }).map((_, i) => (
              <Card
                key={i}
                id={`opponent-hand-${i}`}
                name=""
                type="UNIT"
                manaCost={0}
                faceDown={true}
                selected={false}
                playable={false}
                onClick={() => {}}
              />
            ))}
          </div>
        </div>
        <div className="board__hand-zone__piles">
          <CardPile count={opponent.deckSize} variant="deck" />
          <CardPile count={opponent.graveyard.length} variant="graveyard" />
        </div>
      </div>

      {/* 3. OPPONENT FIELD */}
      <div className="board__opponent-field" data-testid="opponent-field">
        {opponent.field.length === 0 ? (
          <span className="board__field-empty">No units on field</span>
        ) : (
          opponent.field.map((unit) => (
            <FieldUnitCard
              key={unit.card.id}
              unit={unit}
              selected={selectedCardId === unit.card.id}
              onClick={() => onCardClick(unit.card.id)}
            />
          ))
        )}
      </div>

      {/* 4. CENTER STRIP */}
      <div className="board__center-strip" data-testid="center-strip">
        <div className="board__center-item">
          <span className="board__center-label">Turn</span>
          <span className="board__center-value" data-testid="turn-number">{turnNumber}</span>
        </div>

        <div className="board__center-item">
          <span className="board__center-label">Mana</span>
          <span className="board__center-value" data-testid="current-mana">{currentMana}</span>
        </div>

        <div className="board__center-item">
          <span
            className={`board__phase-badge board__phase-badge--${phase}`}
            data-testid="phase-badge"
          >
            {phase}
          </span>
        </div>

        <div className="board__center-item">
          <span
            className={`board__active-indicator${isPlayerActive ? '' : ' board__active-indicator--opponent'}`}
            data-testid="active-indicator"
          >
            {isPlayerActive ? 'Your turn' : `${opponent.username}'s turn`}
          </span>
        </div>
      </div>

      {/* 5. PLAYER FIELD */}
      <div className="board__player-field" data-testid="player-field">
        {player.field.length === 0 ? (
          <span className="board__field-empty">No units on field</span>
        ) : (
          player.field.map((unit) => (
            <FieldUnitCard
              key={unit.card.id}
              unit={unit}
              selected={selectedCardId === unit.card.id}
              onClick={() => onCardClick(unit.card.id)}
            />
          ))
        )}
      </div>

      {/* 6. PLAYER HAND + INFO BAR */}
      <div className="board__player-hand-area">
        <div className="board__hand-zone__spacer" />
        <div className="board__hand-zone__center">
          <div className="board__hand-row" data-testid="player-hand">
            {player.hand.map((card) => (
              <Card
                key={card.id}
                id={card.id}
                name={card.name}
                type={card.type}
                unitClass={card.type === 'UNIT' ? card.unitClass : undefined}
                attack={card.type === 'UNIT' ? card.attack : undefined}
                defense={card.type === 'UNIT' ? card.defense : undefined}
                manaCost={card.manaCost}
                faceDown={false}
                selected={selectedCardId === card.id}
                playable={card.manaCost <= currentMana}
                onClick={() => onCardClick(card.id)}
              />
            ))}
          </div>
          <span className="board__player-username">{player.username}</span>
        </div>
        <div className="board__hand-zone__piles">
          <CardPile count={player.deckSize} variant="deck" />
          <CardPile count={player.graveyard.length} variant="graveyard" />
        </div>
      </div>
    </div>
  );
}