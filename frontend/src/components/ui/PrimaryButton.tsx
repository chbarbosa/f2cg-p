import { useState } from 'react';
import type { CSSProperties } from 'react';

interface PrimaryButtonProps {
  children: React.ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  type?: 'button' | 'submit' | 'reset';
  fullWidth?: boolean;
  active?: boolean;
  className?: string;
}

export function PrimaryButton({
  children,
  onClick,
  disabled = false,
  type = 'button',
  fullWidth = false,
  active = false,
  className,
}: PrimaryButtonProps) {
  const [hovered, setHovered] = useState(false);
  const [pressed, setPressed] = useState(false);

  const isActive = (hovered || active) && !disabled;
  const isPressed = pressed && !disabled;

  const style: CSSProperties = {
    background: isPressed ? '#a8882c' : isActive ? 'var(--color-gold-bright)' : 'var(--color-gold)',
    color: 'var(--color-text-dark)',
    fontSize: '18px',
    fontWeight: 500,
    padding: '14px 40px',
    letterSpacing: isActive ? '3px' : '1px',
    borderRadius: 0,
    border: 'none',
    cursor: disabled ? 'not-allowed' : 'pointer',
    opacity: disabled ? 0.5 : 1,
    transform: isPressed
      ? 'scale(0.96) translateY(1px)'
      : isActive
      ? 'translateY(-2px) scale(1.03)'
      : 'none',
    transition: 'background 0.15s, transform 0.1s, letter-spacing 0.15s',
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