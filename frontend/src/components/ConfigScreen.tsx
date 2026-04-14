import { useState } from 'react';
import { updateProfile } from '../api/player';
import { useAuthStore } from '../store/authStore';
import { CountrySelect } from './CountrySelect';
import { PrimaryButton, TertiaryButton } from './ui';

interface Props {
  onBack: () => void;
}

export function ConfigScreen({ onBack }: Props) {
  const { nickname: savedNickname, country: savedCountry, setProfile } = useAuthStore();
  const [nickname, setNickname] = useState(savedNickname ?? '');
  const [country, setCountry] = useState(savedCountry ?? '');
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.SyntheticEvent) {
    e.preventDefault();
    setError('');
    setSaved(false);
    setLoading(true);
    try {
      await updateProfile(nickname || null, country || null);
      setProfile(nickname || null, country || null);
      setSaved(true);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Something went wrong');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page-center">
      <div className="surface-card surface-card--narrow">
        <h2 className="section-title">Config</h2>
        <form onSubmit={handleSubmit} className="form-stack">
          <label className="form-label">Nickname</label>
          <input
            className="form-input"
            placeholder="Your nickname (max 20)"
            value={nickname}
            onChange={(e) => { setNickname(e.target.value); setSaved(false); }}
            maxLength={20}
          />
          <label className="form-label">Country</label>
          <CountrySelect
            value={country}
            onChange={(code) => { setCountry(code); setSaved(false); }}
          />
          {error && <p className="text-error">{error}</p>}
          {saved && <p className="text-success">Saved!</p>}
          <div style={{ marginTop: '0.5rem' }}>
            <PrimaryButton type="submit" disabled={loading} fullWidth>
              {loading ? '...' : 'Save'}
            </PrimaryButton>
          </div>
          <div style={{ marginTop: '0.5rem' }}>
            <TertiaryButton type="button" onClick={onBack} fullWidth>BACK</TertiaryButton>
          </div>
        </form>
      </div>
    </div>
  );
}