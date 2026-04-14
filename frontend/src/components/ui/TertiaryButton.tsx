import { useState } from 'react';
import type { CSSProperties } from 'react';

interface TertiaryButtonProps {
  children: React.ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  type?: 'button' | 'submit' | 'reset';
  fullWidth?: boolean;
  active?: boolean;
  className?: string;
}

export function TertiaryButton({
  children,
  onClick,
  disabled = false,
  type = 'button',
  fullWidth = false,
  active = false,
  className,
}: TertiaryButtonProps) {
  const [hovered, setHovered] = useState(false);
  const [pressed, setPressed] = useState(false);

  const isActive = (hovered || active) && !disabled;
  const isPressed = pressed && !disabled;

  const style: CSSProperties = {
    background: 'transparent',
    color: isActive ? 'var(--color-gold-muted)' : 'var(--color-text-dim)',
    fontSize: '14px',
    fontWeight: 500,
    padding: '10px 32px',
    letterSpacing: isActive ? '2px' : '1px',
    boxShadow: isActive
      ? 'inset 0 0 0 1px var(--color-gold-muted)'
      : 'inset 0 0 0 1px var(--color-gold-dim)',
    borderRadius: 0,
    border: 'none',
    cursor: disabled ? 'not-allowed' : 'pointer',
    opacity: disabled ? 0.5 : 1,
    transform: isPressed
      ? 'scale(0.97)'
      : isActive
      ? 'translateY(-1px)'
      : 'none',
    transition: 'color 0.15s, transform 0.1s, letter-spacing 0.15s, box-shadow 0.15s',
    width: fullWidth ? '100%' : 'auto',
    display: 'inline-block',
    fontFamily: 'inherit',
  };

  return (
    <button
      type={type}
      style={style}
      onClick={disabled ? undefined : onClick}
      disabled={disabled}
      className={className}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => { setHovered(false); setPressed(false); }}
      onMouseDown={() => setPressed(true)}
      onMouseUp={() => setPressed(false)}
    >
      {children}
    </button>
  );
}