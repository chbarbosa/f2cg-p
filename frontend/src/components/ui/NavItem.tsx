import { useState } from 'react';
import type { CSSProperties } from 'react';

interface NavItemProps {
  children: React.ReactNode;
  onClick?: () => void;
  variant?: 'primary' | 'secondary' | 'tertiary';
  fullWidth?: boolean;
  active?: boolean;
  disabled?: boolean;
  className?: string;
}

const CLIP_PATH = 'polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%)';

const BASE_STYLES: Record<'primary' | 'secondary' | 'tertiary', { background: string; color: string; boxShadow: string }> = {
  primary: {
    background: 'var(--color-gold)',
    color: 'var(--color-text-dark)',
    boxShadow: 'inset 0 0 0 1.5px var(--color-gold-bright)',
  },
  secondary: {
    background: 'var(--color-surface)',
    color: 'var(--color-gold)',
    boxShadow: 'inset 0 0 0 1px var(--color-gold)',
  },
  tertiary: {
    background: 'transparent',
    color: 'var(--color-text-dim)',
    boxShadow: 'inset 0 0 0 1px var(--color-gold-dim)',
  },
};

export function NavItem({
  children,
  onClick,
  variant = 'secondary',
  fullWidth = false,
  active = false,
  disabled = false,
  className,
}: NavItemProps) {
  const [hovered, setHovered] = useState(false);
  const [pressed, setPressed] = useState(false);

  const isActive = (hovered || active) && !disabled;
  const isPressed = pressed;

  const base = BASE_STYLES[variant];

  const style: CSSProperties = {
    background: isActive ? 'var(--color-gold)' : base.background,
    color: isActive ? 'var(--color-text-dark)' : base.color,
    fontSize: '20px',
    fontWeight: 500,
    padding: '18px 40px 18px 28px',
    letterSpacing: isActive ? '4px' : '2px',
    boxShadow: isActive ? 'inset 0 0 0 1px var(--color-gold-bright)' : base.boxShadow,
    clipPath: CLIP_PATH,
    borderRadius: 0,
    border: 'none',
    cursor: disabled ? 'not-allowed' : 'pointer',
    opacity: disabled ? 0.5 : 1,
    transform: isPressed
      ? 'translateX(4px) scale(0.98)'
      : isActive
      ? 'translateX(8px) scale(1.02)'
      : 'none',
    transition: 'all 0.12s ease',
    display: 'block',
    textAlign: 'left',
    width: fullWidth ? '100%' : undefined,
    maxWidth: fullWidth ? 'none' : '380px',
    fontFamily: 'inherit',
  };

  return (
    <button
      type="button"
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