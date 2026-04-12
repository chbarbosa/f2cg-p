import { useState } from 'react';
import { ApiError } from '../api/errors';
import { login, register, verifyAccount } from '../api/auth';
import { useAuthStore } from '../store/authStore';
import { ErrorMessage } from './ErrorMessage';
import { PrimaryButton, SecondaryButton, TertiaryButton } from './ui';

function toFriendlyError(err: unknown, context: 'login' | 'register' | 'verify'): string {
  if (err instanceof ApiError) {
    if (err.status === 401)
      return 'Invalid credentials. Please check your email and password, or create a new account.';
    if (err.status === 403)
      return 'Your account is not activated yet. Check your email for the verification code.';
    if (err.status === 409)
      return 'This email is already registered. Try logging in instead.';
    if (err.status === 400 && context === 'verify')
      return 'Invalid or expired code. Please check the code sent to your email.';
    if (err.status === 400)
      return 'Invalid email format.';
  }
  return 'Something went wrong. Please try again.';
}

export function AuthForm() {
  const [tab, setTab] = useState<'login' | 'register'>('login');
  const [step, setStep] = useState<'auth' | 'verify'>('auth');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const setAuth = useAuthStore((s) => s.login);
  const setPendingEmail = useAuthStore((s) => s.setPendingEmail);
  const sessionExpired = useAuthStore((s) => s.sessionExpired);

  async function handleAuthSubmit(e: React.SyntheticEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (tab === 'login') {
        const res = await login(email, password);
        setAuth(res.playerId, res.token, email, res.nickname ?? null, res.country ?? null);
      } else {
        await register(email, password);
        setPendingEmail(email);
        setStep('verify');
      }
    } catch (err: unknown) {
      setError(toFriendlyError(err, tab === 'login' ? 'login' : 'register'));
    } finally {
      setLoading(false);
    }
  }

  async function handleVerifySubmit(e: React.SyntheticEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await verifyAccount(email, code);
      setAuth(res.playerId, res.token, email, res.nickname ?? null, res.country ?? null);
    } catch (err: unknown) {
      setError(toFriendlyError(err, 'verify'));
    } finally {
      setLoading(false);
    }
  }

  if (step === 'verify') {
    return (
      <div className="surface-card surface-card--narrow">
        <h1 className="auth-title">F2CG</h1>
        <p className="text-muted" style={{ textAlign: 'center' }}>A verification code was sent to <strong>{email}</strong></p>
        <form onSubmit={handleVerifySubmit} className="form-stack">
          <input
            className="form-input"
            placeholder="5-digit code"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            maxLength={5}
            pattern="\d{5}"
            required
            autoFocus
          />
          {error && <ErrorMessage message={error} />}
          <PrimaryButton type="submit" disabled={loading} fullWidth>
            {loading ? '...' : 'Verify'}
          </PrimaryButton>
          <TertiaryButton
            type="button"
            onClick={() => { setStep('auth'); setError(''); setCode(''); }}
          >
            Back
          </TertiaryButton>
        </form>
      </div>
    );
  }

  return (
    <div className="surface-card surface-card--narrow">
      <h1 className="auth-title">F2CG</h1>
      {sessionExpired && <ErrorMessage message="Your session has expired. Please log in again." />}
      <div className="auth-tabs">
        <SecondaryButton
          active={tab === 'login'}
          onClick={() => { setTab('login'); setError(''); }}
        >
          Login
        </SecondaryButton>
        <SecondaryButton
          active={tab === 'register'}
          onClick={() => { setTab('register'); setError(''); }}
        >
          Register
        </SecondaryButton>
      </div>
      <form onSubmit={handleAuthSubmit} className="form-stack">
        <input
          className="form-input"
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          autoFocus
        />
        <input
          className="form-input"
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        {error && <ErrorMessage message={error} />}
        <PrimaryButton type="submit" disabled={loading} fullWidth>
          {loading ? '...' : tab === 'login' ? 'Login' : 'Register'}
        </PrimaryButton>
      </form>
    </div>
  );
}