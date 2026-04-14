import { useEffect, useState } from 'react';
import { getCurrentPerformance, getParticipatedSeasons, getSeasonPerformance } from '../api/performance';
import type { PerformanceResponse, PlayerRank, SeasonSummary } from '../api/types';
import { TertiaryButton } from './ui';

interface Props {
  onBack: () => void;
}

function RankBadge({ rank, label }: { rank: PlayerRank; label: string }) {
  return (
    <div className="badge-row">
      <span className="badge-label">{label}</span>
      <span
        data-testid={`rank-badge-${label.toLowerCase().replace(/\s/g, '-')}`}
        className={`badge badge--${rank.toLowerCase()}`}
      >
        {rank}
      </span>
    </div>
  );
}

function PhaseBadge({ phase }: { phase: 'FREE' | 'RANKED' }) {
  const isFree = phase === 'FREE';
  return (
    <span
      data-testid="phase-badge"
      className={`badge${isFree ? ' badge--free' : ' badge--ranked'}`}
    >
      {isFree ? 'Free Season — open matchmaking' : 'Ranked Season — same rank only'}
    </span>
  );
}

function StatsPanel({
  data,
  isPast,
}: {
  data: PerformanceResponse;
  isPast: boolean;
}) {
  const { season, currentPhase, rank, highestRank, totalMatches, victories, defeats, matchesThisWeek } = data;
  const remaining = 15 - matchesThisWeek;
  const showWeekly = !isPast && currentPhase === 'RANKED';

  return (
    <div className="perf-panel" data-testid="stats-panel">
      <h3 className="perf-season-title">
        Season {season.name ?? '?'} · {season.year}
      </h3>
      <p className="perf-period">
        {season.startDate} – {season.endDate}
      </p>

      {!isPast && currentPhase && (
        <div className="perf-row">
          <PhaseBadge phase={currentPhase} />
        </div>
      )}

      <RankBadge rank={rank} label="Rank" />
      {rank === 'PENDING' && (
        <p className="pending-msg" data-testid="pending-message">
          Rank pending — play at least 15 matches
        </p>
      )}

      <RankBadge rank={highestRank} label="Peak rank" />

      <div className="stats-row" data-testid="stats-row">
        <div className="stat">
          <span className="stat-value">{totalMatches}</span>
          <span className="stat-label">Matches</span>
        </div>
        <div className="stat">
          <span className="stat-value stat-value--win">{victories}</span>
          <span className="stat-label">Victories</span>
        </div>
        <div className="stat">
          <span className="stat-value stat-value--loss">{defeats}</span>
          <span className="stat-label">Defeats</span>
        </div>
      </div>

      {showWeekly && (
        <div
          data-testid="weekly-activity"
          className={`weekly-box${remaining <= 0 ? ' weekly-box--met' : ' weekly-box--warn'}`}
        >
          {remaining <= 0 ? (
            <span data-testid="activity-met">
              Activity requirement met
            </span>
          ) : (
            <span data-testid="activity-warning">
              Play {remaining} more {remaining === 1 ? 'match' : 'matches'} to avoid demotion
            </span>
          )}
        </div>
      )}
    </div>
  );
}

export function PerformanceScreen({ onBack }: Props) {
  const [currentData, setCurrentData] = useState<PerformanceResponse | null>(null);
  const [seasons, setSeasons] = useState<SeasonSummary[]>([]);
  const [selectedYear, setSelectedYear] = useState<number | null>(null);
  const [selectedSeasonId, setSelectedSeasonId] = useState<string>('');
  const [pastData, setPastData] = useState<PerformanceResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [loadingPast, setLoadingPast] = useState(false);

  useEffect(() => {
    Promise.all([getCurrentPerformance(), getParticipatedSeasons()])
      .then(([perf, seas]) => {
        setCurrentData(perf);
        setSeasons(seas);
        setSelectedYear(perf.season.year);
      })
      .catch(err => setError(err.message ?? 'Failed to load performance data'))
      .finally(() => setLoading(false));
  }, []);

  const years = [...new Set(seasons.map(s => s.year))].sort((a, b) => b - a);
  const seasonsForYear = seasons.filter(s => s.year === selectedYear);

  const handleYearSelect = (year: number) => {
    setSelectedYear(year);
    setSelectedSeasonId('');
    setPastData(null);
  };

  const handleSeasonSelect = (seasonId: string) => {
    if (!seasonId || seasonId === currentData?.season.id) {
      setSelectedSeasonId('');
      setPastData(null);
      return;
    }
    setSelectedSeasonId(seasonId);
    setLoadingPast(true);
    getSeasonPerformance(seasonId)
      .then(setPastData)
      .catch(err => setError(err.message ?? 'Failed to load season data'))
      .finally(() => setLoadingPast(false));
  };

  const displayData = selectedSeasonId && pastData ? pastData : currentData;
  const isPast = !!(selectedSeasonId && pastData);

  return (
    <div className="perf-page">
      <div className="perf-header">
        <h2 className="section-title">Performance</h2>
      </div>

      {loading && (
        <div className="perf-center">
          <div className="spinner spinner--sm" />
        </div>
      )}

      {!loading && error && (
        <div className="perf-error-box" data-testid="error-message">{error}</div>
      )}

      {!loading && !error && !currentData && (
        <div className="perf-empty" data-testid="no-season-message">
          No active season at the moment.
        </div>
      )}

      {!loading && !error && currentData && (
        <>
          {years.length > 0 && (
            <div className="year-row" data-testid="year-selector">
              {years.map(year => (
                <TertiaryButton
                  key={year}
                  active={selectedYear === year}
                  onClick={() => handleYearSelect(year)}
                >
                  {year}
                </TertiaryButton>
              ))}
            </div>
          )}

          {seasonsForYear.length > 0 && (
            <div className="season-row" data-testid="season-selector">
              {seasonsForYear.map(s => {
                const isCurrent = s.id === currentData.season.id;
                const isSelected = isCurrent
                  ? !selectedSeasonId
                  : selectedSeasonId === s.id;
                return (
                  <TertiaryButton
                    key={s.id}
                    active={isSelected}
                    onClick={() => handleSeasonSelect(s.id)}
                    className={isCurrent ? 'season-btn--current' : undefined}
                  >
                    {s.name ?? `S${s.seasonNumber}`}
                    {isCurrent && <span className="season-current-dot" />}
                  </TertiaryButton>
                );
              })}
            </div>
          )}

          {loadingPast ? (
            <div className="perf-center"><div className="spinner spinner--sm" /></div>
          ) : (
            displayData && <StatsPanel data={displayData} isPast={isPast} />
          )}
        </>
      )}
      <TertiaryButton onClick={onBack}>BACK</TertiaryButton>
    </div>
  );
}