import { useState } from 'react';
import { useDeckStore } from '../store/deckStore';
import type { DeckTheme } from '../api/types';

interface Props {
  onCancel: () => void;
  onSaved: () => void;
}

const THEMES: DeckTheme[] = ['WARRIOR', 'MAGE', 'CLERIC'];

export function DeckBuilder({ onCancel, onSaved }: Props) {
  const {
    deckName, selectedTheme, selectedCardIds, themeIsLocked,
    availableCards, loadingCards, saving, saveError, isDirty,
    setDeckName, selectTheme, toggleCard, resetBuilder, saveDeck,
  } = useDeckStore();

  const [showResetModal, setShowResetModal] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);

  const handleSave = async () => {
    await saveDeck();
    if (!useDeckStore.getState().saveError) {
      onSaved();
    }
  };

  const handleCancel = () => {
    if (isDirty) {
      setShowCancelModal(true);
    } else {
      onCancel();
    }
  };

  const handleResetConfirm = () => {
    resetBuilder();
    setShowResetModal(false);
  };

  const canSave = deckName.trim().length > 0 && selectedTheme !== null && !saving;
  const cardCount = selectedCardIds.length;

  return (
    <div className="deck-builder">
      {/* Header */}
      <div className="deck-builder-header">
        <input
          className="deck-name-input"
          placeholder="Deck name…"
          value={deckName}
          onChange={e => setDeckName(e.target.value)}
        />
        <span className="deck-counter">{cardCount}/20</span>
        <button
          className={`btn btn--primary-green${canSave ? '' : ' btn--disabled'}`}
          onClick={handleSave}
          disabled={!canSave}
        >
          {saving ? 'Saving…' : cardCount === 20 ? 'Save (Playable)' : 'Save (Draft)'}
        </button>
        <button className="btn btn--ghost" onClick={handleCancel}>Cancel</button>
      </div>

      {saveError && <p className="text-error">{saveError}</p>}

      {/* Theme selector */}
      <div className="theme-row">
        <span className="form-label">Theme:</span>
        {THEMES.map(t => (
          <button
            key={t}
            className={`theme-btn${selectedTheme === t ? ' theme-btn--active' : ''}${themeIsLocked && selectedTheme !== t ? ' btn--disabled' : ''}`}
            onClick={() => selectTheme(t)}
            disabled={themeIsLocked && selectedTheme !== t}
          >
            {t}
          </button>
        ))}
        {selectedCardIds.length > 0 && (
          <button className="btn btn--danger btn--sm ml-auto" onClick={() => setShowResetModal(true)}>
            Reset
          </button>
        )}
      </div>

      {/* Card grid */}
      {loadingCards && <p className="text-muted">Loading cards…</p>}
      {!selectedTheme && !loadingCards && (
        <p className="text-muted">Select a theme to see available cards.</p>
      )}

      <div className="card-grid">
        {availableCards.map(card => {
          const selected = selectedCardIds.includes(card.id);
          const disabled = !selected && cardCount >= 20;
          return (
            <button
              key={card.id}
              className={`card-tile${selected ? ' card-tile--selected' : ''}${disabled ? ' btn--disabled' : ''}`}
              onClick={() => !disabled && toggleCard(card.id)}
              disabled={disabled}
            >
              <span className="card-tile__name">{card.name}</span>
              <span className="card-tile__meta">
                {card.cardType === 'UNIT'
                  ? `ATK ${card.attack} / DEF ${card.defense}`
                  : `${card.effectType?.replace('_', ' ')} +${card.effectValue}`}
              </span>
              <span className="card-tile__mana">Mana: {card.manaCost}</span>
            </button>
          );
        })}
      </div>

      {/* Reset modal */}
      {showResetModal && (
        <div className="modal-overlay">
          <div className="modal-box modal-box--wide">
            <p className="modal-title">Reset will clear all card selections. Continue?</p>
            <div className="modal-actions">
              <button className="btn btn--danger btn--sm" onClick={handleResetConfirm}>Confirm</button>
              <button className="btn btn--ghost btn--sm" onClick={() => setShowResetModal(false)}>Cancel</button>
            </div>
          </div>
        </div>
      )}

      {/* Cancel with unsaved changes modal */}
      {showCancelModal && (
        <div className="modal-overlay">
          <div className="modal-box modal-box--wide">
            <p className="modal-title">You have unsaved changes. Discard and go back?</p>
            <div className="modal-actions">
              <button className="btn btn--danger btn--sm" onClick={onCancel}>Discard</button>
              <button className="btn btn--ghost btn--sm" onClick={() => setShowCancelModal(false)}>Keep editing</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}