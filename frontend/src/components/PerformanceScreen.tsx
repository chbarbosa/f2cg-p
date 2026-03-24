import { useEffect, useState } from 'react';
import { getCurrentPerformance, getParticipatedSeasons, getSeasonPerformance } from '../api/performance';
import type { PerformanceResponse, PlayerRank, SeasonSummary } from '../api/types';

interface Props {
  onBack: () => void;
}

const RANK_COLORS: Record<PlayerRank, string> = {
  ELITE:        '#f9e2af',
  ADVANCED:     '#bac2de',
  INTERMEDIATE: '#89b4fa',
  ROOKIE:       '#6c7086',
  PENDING:      '#45475a',
};

function RankBadge({ rank, label }: { rank: PlayerRank; label: string }) {
  return (
    <div style={styles.badgeRow}>
      <span style={styles.badgeLabel}>{label}</span>
      <span
        data-testid={`rank-badge-${label.toLowerCase().replace(/\s/g, '-')}`}
        style={{ ...styles.badge, background: RANK_COLORS[rank], color: '#1e1e2e' }}
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
      style={{
        ...styles.badge,
        background: isFree ? '#89b4fa' : '#cba6f7',
        color: '#1e1e2e',
      }}
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
    <div style={styles.panel} data-testid="stats-panel">
      <h3 style={styles.seasonTitle}>
        Season {season.name ?? '?'} · {season.year}
      </h3>
      <p style={styles.period}>
        {season.startDate} – {season.endDate}
      </p>

      {!isPast && currentPhase && (
        <div style={styles.row}>
          <PhaseBadge phase={currentPhase} />
        </div>
      )}

      <RankBadge rank={rank} label="Rank" />
      {rank === 'PENDING' && (
        <p style={styles.pendingMsg} data-testid="pending-message">
          Rank pending — play at least 15 matches
        </p>
      )}

      <RankBadge rank={highestRank} label="Peak rank" />

      <div style={styles.statsRow} data-testid="stats-row">
        <div style={styles.stat}>
          <span style={styles.statValue}>{totalMatches}</span>
          <span style={styles.statLabel}>Matches</span>
        </div>
        <div style={styles.stat}>
          <span style={{ ...styles.statValue, color: '#a6e3a1' }}>{victories}</span>
          <span style={styles.statLabel}>Victories</span>
        </div>
        <div style={styles.stat}>
          <span style={{ ...styles.statValue, color: '#f38ba8' }}>{defeats}</span>
          <span style={styles.statLabel}>Defeats</span>
        </div>
      </div>

      {showWeekly && (
        <div
          data-testid="weekly-activity"
          style={{
            ...styles.weeklyBox,
            background: remaining <= 0 ? '#1a3a2a' : '#3a2a1a',
            borderColor: remaining <= 0 ? '#a6e3a1' : '#f9e2af',
          }}
        >
          {remaining <= 0 ? (
            <span style={{ color: '#a6e3a1' }} data-testid="activity-met">
              Activity requirement met
            </span>
          ) : (
            <span style={{ color: '#f9e2af' }} data-testid="activity-warning">
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
      })
      .catch(err => setError(err.message ?? 'Failed to load performance data'))
      .finally(() => setLoading(false));
  }, []);

  const handleSeasonChange = (seasonId: string) => {
    setSelectedSeasonId(seasonId);
    if (!seasonId) {
      setPastData(null);
      return;
    }
    setLoadingPast(true);
    getSeasonPerformance(seasonId)
      .then(setPastData)
      .catch(err => setError(err.message ?? 'Failed to load season data'))
      .finally(() => setLoadingPast(false));
  };

  const displayData = selectedSeasonId && pastData ? pastData : currentData;
  const isPast = !!(selectedSeasonId && pastData);

  return (
    <div style={styles.page}>
      <div style={styles.header}>
        <button style={styles.backBtn} onClick={onBack}>← Back</button>
        <h2 style={styles.title}>Performance</h2>
      </div>

      {loading && (
        <div style={styles.center}>
          <div style={styles.spinner} />
        </div>
      )}

      {!loading && error && (
        <div style={styles.errorBox} data-testid="error-message">{error}</div>
      )}

      {!loading && !error && !currentData && (
        <div style={styles.empty} data-testid="no-season-message">
          No active season at the moment.
        </div>
      )}

      {!loading && !error && currentData && (
        <>
          {seasons.length > 0 && (
            <div style={styles.selectorRow}>
              <label style={styles.selectorLabel} htmlFor="season-select">Season:</label>
              <select
                id="season-select"
                data-testid="season-selector"
                style={styles.select}
                value={selectedSeasonId}
                onChange={e => handleSeasonChange(e.target.value)}
              >
                <option value="">Current season</option>
                {seasons.map(s => (
                  <option key={s.id} value={s.id}>
                    Season {s.name ?? s.seasonNumber} · {s.year}
                  </option>
                ))}
              </select>
            </div>
          )}

          {loadingPast ? (
            <div style={styles.center}><div style={styles.spinner} /></div>
          ) : (
            displayData && <StatsPanel data={displayData} isPast={isPast} />
          )}
        </>
      )}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  page: {
    minHeight: '100vh',
    background: '#181825',
    padding: '2rem',
    maxWidth: 600,
    margin: '0 auto',
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    gap: '1rem',
    marginBottom: '1.5rem',
  },
  backBtn: {
    background: 'none',
    border: '1px solid #313244',
    color: '#cdd6f4',
    borderRadius: 6,
    padding: '0.4rem 0.8rem',
    cursor: 'pointer',
    fontSize: '0.9rem',
  },
  title: {
    margin: 0,
    color: '#cdd6f4',
    fontSize: '1.4rem',
  },
  panel: {
    background: '#1e1e2e',
    border: '1px solid #313244',
    borderRadius: 12,
    padding: '1.5rem',
    display: 'flex',
    flexDirection: 'column',
    gap: '1rem',
  },
  seasonTitle: {
    margin: 0,
    color: '#cdd6f4',
    fontSize: '1.1rem',
  },
  period: {
    margin: 0,
    color: '#a6adc8',
    fontSize: '0.85rem',
  },
  row: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
  },
  badgeRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  badgeLabel: {
    color: '#a6adc8',
    fontSize: '0.9rem',
  },
  badge: {
    padding: '0.25rem 0.75rem',
    borderRadius: 20,
    fontWeight: 700,
    fontSize: '0.85rem',
  },
  pendingMsg: {
    margin: 0,
    color: '#a6adc8',
    fontSize: '0.85rem',
    fontStyle: 'italic',
  },
  statsRow: {
    display: 'flex',
    gap: '1rem',
    justifyContent: 'space-around',
    background: '#181825',
    borderRadius: 8,
    padding: '0.75rem',
  },
  stat: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 2,
  },
  statValue: {
    fontSize: '1.4rem',
    fontWeight: 700,
    color: '#cdd6f4',
  },
  statLabel: {
    fontSize: '0.75rem',
    color: '#a6adc8',
  },
  weeklyBox: {
    borderRadius: 8,
    border: '1px solid',
    padding: '0.6rem 1rem',
    fontSize: '0.9rem',
  },
  selectorRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    marginBottom: '1rem',
  },
  selectorLabel: {
    color: '#a6adc8',
    fontSize: '0.9rem',
  },
  select: {
    background: '#1e1e2e',
    border: '1px solid #313244',
    color: '#cdd6f4',
    borderRadius: 6,
    padding: '0.4rem 0.6rem',
    fontSize: '0.9rem',
    cursor: 'pointer',
    flex: 1,
  },
  center: {
    display: 'flex',
    justifyContent: 'center',
    padding: '3rem',
  },
  spinner: {
    width: 32,
    height: 32,
    border: '3px solid #313244',
    borderTop: '3px solid #89b4fa',
    borderRadius: '50%',
    animation: 'spin 0.8s linear infinite',
  },
  errorBox: {
    background: '#2a1a1e',
    border: '1px solid #f38ba8',
    color: '#f38ba8',
    borderRadius: 8,
    padding: '1rem',
  },
  empty: {
    color: '#a6adc8',
    textAlign: 'center',
    padding: '3rem',
  },
};