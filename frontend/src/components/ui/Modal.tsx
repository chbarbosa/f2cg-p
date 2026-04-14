import { useEffect, useRef, useState } from 'react';
import type { CSSProperties } from 'react';
import ReactDOM from 'react-dom';
import { PrimaryButton } from './PrimaryButton';
import { SecondaryButton } from './SecondaryButton';
import { DangerButton } from './DangerButton';
import './Modal.css';

interface ModalProps {
  isOpen: boolean;
  title: string;
  message: string;
  confirmLabel: string;
  cancelLabel?: string;
  variant?: 'default' | 'danger';
  onConfirm: () => void;
  onCancel: () => void;
  isLoading?: boolean;
}

export function Modal({
  isOpen,
  title,
  message,
  confirmLabel,
  cancelLabel = 'CANCEL',
  variant = 'default',
  onConfirm,
  onCancel,
  isLoading = false,
}: ModalProps) {
  const [closing, setClosing] = useState(false);
  const [visible, setVisible] = useState(isOpen);
  const boxRef = useRef<HTMLDivElement>(null);

  // Sync open state: when isOpen flips to true, make visible immediately.
  useEffect(() => {
    if (isOpen) {
      setClosing(false);
      setVisible(true);
    }
  }, [isOpen]);

  // Escape key handler
  useEffect(() => {
    if (!visible) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !isLoading) {
        triggerClose();
      }
      if (e.key === 'Tab') {
        trapFocus(e);
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  });

  // Focus trap
  function trapFocus(e: KeyboardEvent) {
    if (!boxRef.current) return;
    const focusable = boxRef.current.querySelectorAll<HTMLElement>(
      'button:not([disabled]), [href], input:not([disabled]), [tabindex]:not([tabindex="-1"])'
    );
    const first = focusable[0];
    const last = focusable[focusable.length - 1];

    if (e.shiftKey) {
      if (document.activeElement === first) {
        e.preventDefault();
        last?.focus();
      }
    } else {
      if (document.activeElement === last) {
        e.preventDefault();
        first?.focus();
      }
    }
  }

  function triggerClose() {
    setClosing(true);
  }

  function handleOverlayAnimationEnd() {
    if (closing) {
      setVisible(false);
      setClosing(false);
      onCancel();
    }
  }

  function handleOverlayClick(e: React.MouseEvent<HTMLDivElement>) {
    if (isLoading) return;
    if (e.target === e.currentTarget) {
      triggerClose();
    }
  }

  if (!visible) return null;

  const isDanger = variant === 'danger';

  const overlayStyle: CSSProperties = {
    background: 'rgba(0, 0, 0, 0.75)',
  };

  const boxStyle: CSSProperties = {
    border: `1px solid ${isDanger ? 'var(--color-danger-border)' : 'var(--color-gold)'}`,
  };

  const titleStyle: CSSProperties = {
    background: isDanger ? 'var(--color-danger-bg)' : 'var(--color-gold)',
    color: isDanger ? 'var(--color-danger-text)' : 'var(--color-text-dark)',
  };

  const bodyStyle: CSSProperties = {
    color: 'var(--color-gold)',
  };

  const overlayClass = `modal-portal-overlay${closing ? ' modal-portal-overlay--closing' : ''}`;

  const content = (
    <div
      className={overlayClass}
      style={overlayStyle}
      onClick={handleOverlayClick}
      onAnimationEnd={handleOverlayAnimationEnd}
      data-testid="modal-overlay"
    >
      <div
        className="modal-portal-box"
        style={boxStyle}
        ref={boxRef}
        data-testid="modal"
      >
        <div className="modal-portal-title" style={titleStyle} data-testid="modal-title">
          {title}
        </div>
        <div className="modal-portal-body" style={bodyStyle} data-testid="modal-message">
          {message}
        </div>
        <div className="modal-portal-footer">
          <span data-testid="modal-cancel">
            <SecondaryButton onClick={isLoading ? undefined : triggerClose} disabled={isLoading}>
              {cancelLabel}
            </SecondaryButton>
          </span>
          <span data-testid="modal-confirm">
            {isDanger ? (
              <DangerButton onClick={onConfirm} disabled={isLoading}>
                {isLoading ? `${confirmLabel}...` : confirmLabel}
              </DangerButton>
            ) : (
              <PrimaryButton onClick={onConfirm} disabled={isLoading}>
                {isLoading ? `${confirmLabel}...` : confirmLabel}
              </PrimaryButton>
            )}
          </span>
        </div>
      </div>
    </div>
  );

  return ReactDOM.createPortal(content, document.body);
}