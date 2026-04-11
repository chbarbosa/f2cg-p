import './Card.css';

interface CardProps {
  id: string;
  name: string;
  type: 'UNIT' | 'BUFF' | 'DEBUFF';
  unitClass?: string;
  attack?: number;
  defense?: number;
  manaCost: number;
  faceDown: boolean;
  selected: boolean;
  playable: boolean;
  activeBuff?: { name: string };
  activeDebuff?: { name: string };
  onClick: () => void;
}

export function Card({
  id,
  name,
  type,
  attack,
  defense,
  manaCost,
  faceDown,
  selected,
  playable,
  activeBuff,
  activeDebuff,
  onClick,
}: CardProps) {
  const classNames = [
    'card',
    faceDown ? 'card--face-down' : '',
    !faceDown && selected ? 'card--selected' : '',
    !faceDown && !playable ? 'card--unplayable' : '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div className="card-wrapper" data-testid={`card-${id}`}>
      {!faceDown && activeBuff && (
        <span className="card-peek card-peek--buff" data-testid="buff-peek">
          {activeBuff.name}
        </span>
      )}
      {!faceDown && activeDebuff && (
        <span className="card-peek card-peek--debuff" data-testid="debuff-peek">
          {activeDebuff.name}
        </span>
      )}

      <div className={classNames} onClick={faceDown ? undefined : onClick}>
        {!faceDown && (
          <>
            <span className="card__mana-badge">{manaCost}</span>
            <div className="card__body">
              <span className="card__name">{name}</span>
              <span className="card__type">{type}</span>
            </div>
            {type === 'UNIT' && (
              <div className="card__atk-def">
                <span className="card__atk">{attack}</span>
                <span className="card__def">{defense}</span>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}