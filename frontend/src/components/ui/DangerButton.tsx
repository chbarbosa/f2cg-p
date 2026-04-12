import { useState } from 'react';
import type { CSSProperties } from 'react';

interface DangerButtonProps {
  children: React.ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  type?: 'button' | 'submit' | 'reset';
  fullWidth?: boolean;
  active?: boolean;
  className?: string;
}

const CLIP_PATH = 'polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%)';

export function DangerButton({
  children,
  onClick,
  disabled = false,
  type = 'button',
  fullWidth = false,
  active = false,
  className,
}: DangerButtonProps) {
  const [hovered, setHovered] = useState(false);
  const [pressed, setPressed] = useState(false);

  const isActive = (hovered || active) && !disabled;
  const isPressed = pressed && !disabled;

  const style: CSSProperties = {
    background: isActive ? 'var(--color-danger-hover-bg)' : 'var(--color-danger-bg)',
    color: isActive ? 'var(--color-danger-hover-text)' : 'var(--color-danger-text)',
    fontSize: '15px',
    fontWeight: 500,
    padding: '11px 34px',
    letterSpacing: isActive ? '3px' : '1px',
    boxShadow: isActive
      ? 'inset 0 0 0 1.5px var(--color-danger-hover-border)'
      : 'inset 0 0 0 1px var(--color-danger-border)',
    clipPath: CLIP_PATH,
    borderRadius: 0,
    border: 'none',
    cursor: disabled ? 'not-allowed' : 'pointer',
    opacity: disabled ? 0.5 : 1,
    transform: isPressed
      ? 'scale(0.96)'
      : isActive
      ? 'translateY(-2px) scale(1.02)'
      : 'none',
    transition: 'background 0.15s, transform 0.1s, letter-spacing 0.15s, color 0.15s, box-shadow 0.15s',
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