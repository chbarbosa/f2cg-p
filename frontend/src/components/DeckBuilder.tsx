import React, { useState } from 'react';
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
    <div style={styles.container}>
      {/* Header */}
      <div style={styles.header}>
        <input
          style={styles.nameInput}
          placeholder="Deck name…"
          value={deckName}
          onChange={e => setDeckName(e.target.value)}
        />
        <span style={styles.counter}>{cardCount}/20</span>
        <button
          style={canSave ? styles.saveBtn : { ...styles.saveBtn, opacity: 0.4, cursor: 'not-allowed' }}
          onClick={handleSave}
          disabled={!canSave}
        >
          {saving ? 'Saving…' : cardCount === 20 ? 'Save (Playable)' : 'Save (Draft)'}
        </button>
        <button style={styles.cancelBtn} onClick={handleCancel}>Cancel</button>
      </div>

      {saveError && <p style={styles.error}>{saveError}</p>}

      {/* Theme selector */}
      <div style={styles.themeRow}>
        <span style={styles.label}>Theme:</span>
        {THEMES.map(t => (
          <button
            key={t}
            style={
              selectedTheme === t
                ? { ...styles.themeBtn, ...styles.themeBtnActive }
                : themeIsLocked
                ? { ...styles.themeBtn, opacity: 0.4, cursor: 'not-allowed' }
                : styles.themeBtn
            }
            onClick={() => selectTheme(t)}
            disabled={themeIsLocked && selectedTheme !== t}
          >
            {t}
          </button>
        ))}
        {selectedCardIds.length > 0 && (
          <button style={styles.resetBtn} onClick={() => setShowResetModal(true)}>
            Reset
          </button>
        )}
      </div>

      {/* Card grid */}
      {loadingCards && <p style={styles.muted}>Loading cards…</p>}
      {!selectedTheme && !loadingCards && (
        <p style={styles.muted}>Select a theme to see available cards.</p>
      )}

      <div style={styles.grid}>
        {availableCards.map(card => {
          const selected = selectedCardIds.includes(card.id);
          const disabled = !selected && cardCount >= 20;
          return (
            <button
              key={card.id}
              style={
                selected
                  ? { ...styles.cardTile, ...styles.cardSelected }
                  : disabled
                  ? { ...styles.cardTile, opacity: 0.4, cursor: 'not-allowed' }
                  : styles.cardTile
              }
              onClick={() => !disabled && toggleCard(card.id)}
              disabled={disabled}
            >
              <span style={styles.cardName}>{card.name}</span>
              <span style={styles.cardMeta}>
                {card.cardType === 'UNIT'
                  ? `ATK ${card.attack} / DEF ${card.defense}`
                  : `${card.effectType?.replace('_', ' ')} +${card.effectValue}`}
              </span>
              <span style={styles.manaCost}>Mana: {card.manaCost}</span>
            </button>
          );
        })}
      </div>

      {/* Reset modal */}
      {showResetModal && (
        <div style={styles.overlay}>
          <div style={styles.modal}>
            <p style={styles.modalText}>Reset will clear all card selections. Continue?</p>
            <div style={styles.modalButtons}>
              <button style={styles.modalConfirm} onClick={handleResetConfirm}>Confirm</button>
              <button style={styles.modalCancel} onClick={() => setShowResetModal(false)}>Cancel</button>
            </div>
          </div>
        </div>
      )}

      {/* Cancel with unsaved changes modal */}
      {showCancelModal && (
        <div style={styles.overlay}>
          <div style={styles.modal}>
            <p style={styles.modalText}>You have unsaved changes. Discard and go back?</p>
            <div style={styles.modalButtons}>
              <button style={styles.modalConfirm} onClick={onCancel}>Discard</button>
              <button style={styles.modalCancel} onClick={() => setShowCancelModal(false)}>Keep editing</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: { width: '100%', maxWidth: 900, margin: '0 auto', padding: '1.5rem' },
  header: { display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1rem' },
  nameInput: {
    flex: 1,
    padding: '0.5rem 0.75rem',
    borderRadius: 6,
    border: '1px solid #313244',
    background: '#181825',
    color: '#cdd6f4',
    fontSize: '1rem',
    outline: 'none',
  },
  counter: {
    color: '#cdd6f4',
    fontWeight: 700,
    fontSize: '1.1rem',
    minWidth: 60,
    textAlign: 'center',
  },
  saveBtn: {
    padding: '0.5rem 1.2rem',
    borderRadius: 6,
    border: 'none',
    background: '#a6e3a1',
    color: '#1e1e2e',
    fontWeight: 700,
    cursor: 'pointer',
  },
  cancelBtn: {
    padding: '0.5rem 1rem',
    borderRadius: 6,
    border: 'none',
    background: '#313244',
    color: '#cdd6f4',
    cursor: 'pointer',
  },
  error: { color: '#f38ba8', marginBottom: '0.5rem' },
  themeRow: { display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.25rem' },
  label: { color: '#a6adc8', fontSize: '0.9rem' },
  themeBtn: {
    padding: '0.4rem 1rem',
    borderRadius: 6,
    border: '1px solid #313244',
    background: '#1e1e2e',
    color: '#cdd6f4',
    cursor: 'pointer',
    fontWeight: 600,
  },
  themeBtnActive: {
    background: '#89b4fa',
    color: '#1e1e2e',
    borderColor: '#89b4fa',
  },
  resetBtn: {
    padding: '0.4rem 0.9rem',
    borderRadius: 6,
    border: 'none',
    background: '#f38ba8',
    color: '#1e1e2e',
    cursor: 'pointer',
    fontWeight: 600,
    marginLeft: 'auto',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))',
    gap: '0.75rem',
  },
  cardTile: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.3rem',
    padding: '0.75rem',
    background: '#1e1e2e',
    border: '2px solid #313244',
    borderRadius: 8,
    cursor: 'pointer',
    textAlign: 'left',
  },
  cardSelected: {
    border: '2px solid #89b4fa',
    background: '#1a1a2e',
  },
  cardName: { color: '#cdd6f4', fontWeight: 600, fontSize: '0.9rem' },
  cardMeta: { color: '#a6adc8', fontSize: '0.75rem' },
  manaCost: { color: '#89dceb', fontSize: '0.75rem' },
  muted: { color: '#a6adc8', fontSize: '0.9rem' },
  overlay: {
    position: 'fixed',
    inset: 0,
    background: 'rgba(0,0,0,0.6)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 100,
  },
  modal: {
    background: '#1e1e2e',
    border: '1px solid #313244',
    borderRadius: 10,
    padding: '2rem',
    maxWidth: 380,
    width: '90%',
  },
  modalText: { color: '#cdd6f4', marginBottom: '1.5rem', lineHeight: 1.5 },
  modalButtons: { display: 'flex', gap: '1rem', justifyContent: 'flex-end' },
  modalConfirm: {
    padding: '0.5rem 1.2rem',
    borderRadius: 6,
    border: 'none',
    background: '#f38ba8',
    color: '#1e1e2e',
    fontWeight: 700,
    cursor: 'pointer',
  },
  modalCancel: {
    padding: '0.5rem 1rem',
    borderRadius: 6,
    border: 'none',
    background: '#313244',
    color: '#cdd6f4',
    cursor: 'pointer',
  },
};