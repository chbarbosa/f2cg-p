import { useEffect, useState } from 'react';
import { useQueueStore } from '../store/queueStore';
import { useAuthStore } from '../store/authStore';
import { useQueueSSE } from '../hooks/useQueueSSE';
import { DangerButton, SecondaryButton } from './ui';

interface Props {
  onCancelled: () => void;
  onMatchFound: (gamePublicId: string) => void;
}

export function QueueWaiting({ onCancelled, onMatchFound }: Props) {
  const { cancel, loading } = useQueueStore();
  const [showConfirm, setShowConfirm] = useState(false);
  const { matchFound, timedOut } = useQueueSSE();

  useEffect(() => {
    if (matchFound) {
      onMatchFound(matchFound.gamePublicId);
    }
  }, [matchFound, onMatchFound]);

  const handleConfirmCancel = async () => {
    await cancel();
    setShowConfirm(false);
    onCancelled();
  };

  const handleBackToHome = async () => {
    await cancel();
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
    <div className="page-center">
      <div className="surface-card surface-card--wide">
        <div className="spinner" aria-label="spinner" />
        <h2 className="section-title">Looking for opponent…</h2>
        <p className="text-muted">You're in the queue. Hang tight!</p>
        <div style={{ marginTop: '0.5rem' }}>
          <DangerButton
            onClick={() => setShowConfirm(true)}
            disabled={loading}
          >
            Cancel
          </DangerButton>
        </div>
      </div>

      {showConfirm && (
        <div className="modal-overlay" onClick={() => setShowConfirm(false)}>
          <div className="modal-box" onClick={e => e.stopPropagation()}>
            <p className="modal-title">Leave the queue?</p>
            <p className="modal-sub">You'll lose your place and have to rejoin.</p>
            <div className="modal-actions">
              <SecondaryButton onClick={() => setShowConfirm(false)}>Stay</SecondaryButton>
              <DangerButton onClick={handleConfirmCancel}>Leave</DangerButton>
            </div>
          </div>
        </div>
      )}

      {timedOut && (
        <div className="modal-overlay">
          <div className="modal-box">
            <p className="modal-title">No opponent found</p>
            <p className="modal-sub">We could not find an opponent. Please try again.</p>
            <div className="modal-actions">
              <DangerButton onClick={handleBackToHome}>Back to Home</DangerButton>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}