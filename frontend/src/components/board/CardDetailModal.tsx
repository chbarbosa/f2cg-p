import { useEffect } from 'react';
import type { Card, EffectType, FieldUnit } from '../../api/gameTypes';
import './CardDetailModal.css';

interface Props {
  card: Card | null;
  fieldUnit?: FieldUnit;
  onClose: () => void;
}

function formatEffect(type: EffectType, value: number): string {
  switch (type) {
    case 'ATK_BOOST':     return `+${value} ATK`;
    case 'DEF_BOOST':     return `+${value} DEF`;
    case 'ATK_REDUCTION': return `-${value} ATK`;
    case 'DEF_REDUCTION': return `-${value} DEF`;
  }
}

export function CardDetailModal({ card, fieldUnit, onClose }: Props) {
  useEffect(() => {
    if (!card) return;
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  }, [card, onClose]);

  if (!card) return null;

  const currentAtk = fieldUnit?.currentAttack;
  const currentDef = fieldUnit?.currentDefense;
  const baseAtk = card.type === 'UNIT' ? card.attack : undefined;
  const baseDef = card.type === 'UNIT' ? card.defense : undefined;
  const atkModified = currentAtk !== undefined && baseAtk !== undefined && currentAtk !== baseAtk;
  const defModified = currentDef !== undefined && baseDef !== undefined && currentDef !== baseDef;

  return (
    <div
      className="card-modal-overlay"
      data-testid="modal-overlay"
      onClick={onClose}
    >
      <div
        className={`card-modal card-modal--${card.type}`}
        data-testid="card-detail-modal"
        onClick={e => e.stopPropagation()}
      >
        {/* Top strip: mana badge (left) + type label (right) */}
        <div className="card-modal__top-strip">
          <span className="card-modal__mana-badge">{card.manaCost}</span>
          <span className="card-modal__type-label">{card.type}</span>
        </div>

        {/* Name + class */}
        <div className="card-modal__header">
          <h2 className="card-modal__name" data-testid="modal-card-name">{card.name}</h2>
          {card.type === 'UNIT' && (
            <span className={`class-badge class-badge--${card.unitClass}`}>
              {card.unitClass}
            </span>
          )}
        </div>

        {/* Scrollable body */}
        <div className="card-modal__body">
          {card.type === 'UNIT' && (
            <>
              {/* Active buff */}
              {fieldUnit?.activeBuff && (
                <div className="card-modal__buff" data-testid="modal-buff">
                  <span className="card-modal__buff-name">{fieldUnit.activeBuff.name}</span>
                  <span className="card-modal__effect-text">
                    {formatEffect(fieldUnit.activeBuff.effect.type, fieldUnit.activeBuff.effect.value)}
                  </span>
                </div>
              )}

              {/* Active debuff */}
              {fieldUnit?.activeDebuff && (
                <div className="card-modal__debuff" data-testid="modal-debuff">
                  <span className="card-modal__debuff-name">{fieldUnit.activeDebuff.name}</span>
                  <span className="card-modal__effect-text">
                    {formatEffect(fieldUnit.activeDebuff.effect.type, fieldUnit.activeDebuff.effect.value)}
                  </span>
                </div>
              )}

              {/* Abilities */}
              {card.abilities.length > 0 && (
                <div>
                  <p className="card-modal__section-label">Abilities</p>
                  <div className="card-modal__abilities" data-testid="modal-abilities">
                    {card.abilities.map(ability => (
                      <div key={ability.id} className="card-modal__ability">
                        <div className="card-modal__ability-header">
                          <span className="card-modal__ability-name">{ability.name}</span>
                          <span className={`ability-type-badge ability-type-badge--${ability.type}`}>
                            {ability.type}
                          </span>
                        </div>
                        <span className="card-modal__ability-desc">{ability.description}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Has acted banner */}
              {fieldUnit?.hasActed && (
                <div className="card-modal__acted-banner" data-testid="modal-acted-banner">
                  Already acted this turn
                </div>
              )}
            </>
          )}

          {/* BUFF / DEBUFF body */}
          {(card.type === 'BUFF' || card.type === 'DEBUFF') && (
            <>
              <p className="card-modal__effect-desc">
                {formatEffect(card.effect.type, card.effect.value)}
              </p>
              <p className="card-modal__flavor">— no flavor text —</p>
            </>
          )}
        </div>

        {/* Footer: ATK / DEF pinned at bottom (units only) */}
        {card.type === 'UNIT' && (
          <div className="card-modal__footer" data-testid="modal-base-stats">
            <div className="card-modal__stat">
              <span className="card-modal__stat-label">ATK</span>
              <span
                className="card-modal__stat-value card-modal__stat-value--atk"
                data-testid="modal-current-atk"
              >
                {currentAtk ?? card.attack}
              </span>
              {atkModified && (
                <span className="card-modal__base-stat" data-testid="modal-base-atk">
                  {card.attack}
                </span>
              )}
            </div>

            <div className="card-modal__footer-divider" />

            <div className="card-modal__stat">
              <span className="card-modal__stat-label">DEF</span>
              <span
                className="card-modal__stat-value card-modal__stat-value--def"
                data-testid="modal-current-def"
              >
                {currentDef ?? card.defense}
              </span>
              {defModified && (
                <span className="card-modal__base-stat" data-testid="modal-base-def">
                  {card.defense}
                </span>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}