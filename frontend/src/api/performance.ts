import { get } from './http';
import type { PerformanceResponse, SeasonSummary } from './types';

export function getCurrentPerformance(): Promise<PerformanceResponse> {
  return get<PerformanceResponse>('/api/performance/current');
}

export function getSeasonPerformance(seasonId: string): Promise<PerformanceResponse> {
  return get<PerformanceResponse>(`/api/performance?seasonId=${encodeURIComponent(seasonId)}`);
}

export function getParticipatedSeasons(): Promise<SeasonSummary[]> {
  return get<SeasonSummary[]>('/api/performance/seasons');
}