import React, { useEffect, useState } from 'react';
import { useQueueStore } from '../store/queueStore';
import { useAuthStore } from '../store/authStore';

interface Props {
  onCancelled: () => void;
}

export function QueueWaiting({ onCancelled }: Props) {
  const { cancel, loading } = useQueueStore();
  const [showConfirm, setShowConfirm] = useState(false);

  const handleConfirmCancel = async () => {
    await cancel();
    setShowConfirm(false);
    onCancelled();
  };

  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      e.preventDefault();
      e.returnValue = '';
      const token = useAuthStore.getState().token;
      if (token) {
        fetch('/api/queue', {
          method: 'DELETE',
          keepalive: true,
          headers: { Authorization: `Bearer ${token}` },
        });
      }
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, []);

  return (
    <div style={styles.center}>
      <div style={styles.card}>
        <div style={styles.spinner} aria-label="spinner" />
        <h2 style={styles.title}>Looking for opponent…</h2>
        <p style={styles.sub}>You're in the queue. Hang tight!</p>
        <button
          style={styles.cancelBtn}
          onClick={() => setShowConfirm(true)}
          disabled={loading}
        >
          Cancel
        </button>
      </div>

      {showConfirm && (
        <div style={styles.overlay} onClick={() => setShowConfirm(false)}>
          <div style={styles.modal} onClick={e => e.stopPropagation()}>
            <p style={styles.modalTitle}>Leave the queue?</p>
            <p style={styles.modalSub}>You'll lose your place and have to rejoin.</p>
            <div style={styles.modalActions}>
              <button style={styles.stayBtn} onClick={() => setShowConfirm(false)}>Stay</button>
              <button style={styles.confirmBtn} onClick={handleConfirmCancel}>Leave</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  center: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    background: '#181825',
  },
  card: {
    background: '#1e1e2e',
    border: '1px solid #313244',
    borderRadius: 12,
    padding: '2.5rem 2rem',
    width: 340,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '1rem',
  },
  spinner: {
    width: 48,
    height: 48,
    borderRadius: '50%',
    border: '4px solid #313244',
    borderTopColor: '#89b4fa',
    animation: 'spin 1s linear infinite',
  },
  title: { margin: 0, color: '#cdd6f4', fontSize: '1.2rem', textAlign: 'center' },
  sub: { margin: 0, color: '#a6adc8', fontSize: '0.9rem', textAlign: 'center' },
  cancelBtn: {
    marginTop: '0.5rem',
    padding: '0.5rem 1.5rem',
    borderRadius: 6,
    border: 'none',
    background: '#f38ba8',
    color: '#1e1e2e',
    fontWeight: 700,
    cursor: 'pointer',
  },
  overlay: {
    position: 'fixed',
    inset: 0,
    background: 'rgba(0,0,0,0.6)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
  },
  modal: {
    background: '#1e1e2e',
    border: '1px solid #45475a',
    borderRadius: 10,
    padding: '1.5rem',
    minWidth: 300,
    boxShadow: '0 8px 32px rgba(0,0,0,0.5)',
  },
  modalTitle: { margin: '0 0 0.5rem', color: '#cdd6f4', fontWeight: 700, fontSize: '1.05rem' },
  modalSub: { margin: '0 0 1.25rem', color: '#a6adc8', fontSize: '0.9rem' },
  modalActions: { display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' },
  stayBtn: {
    padding: '0.3rem 0.8rem',
    borderRadius: 4,
    border: 'none',
    background: '#313244',
    color: '#cdd6f4',
    cursor: 'pointer',
    fontSize: '0.85rem',
  },
  confirmBtn: {
    padding: '0.3rem 0.8rem',
    borderRadius: 4,
    border: 'none',
    background: '#f38ba8',
    color: '#1e1e2e',
    cursor: 'pointer',
    fontSize: '0.85rem',
    fontWeight: 700,
  },
};