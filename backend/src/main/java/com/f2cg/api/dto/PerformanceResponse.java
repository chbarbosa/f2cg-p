package com.f2cg.api.dto;

import com.f2cg.domain.season.PlayerSeasonStats;
import com.f2cg.domain.season.Season;
import com.f2cg.domain.season.SeasonPhase;

public record PerformanceResponse(
        SeasonSummaryResponse season,
        String currentPhase,
        String rank,
        String highestRank,
        int totalMatches,
        int victories,
        int defeats,
        int matchesThisWeek
) {
    public static PerformanceResponse from(Season season, PlayerSeasonStats stats, SeasonPhase phase) {
        return new PerformanceResponse(
                SeasonSummaryResponse.from(season),
                phase != null ? phase.name() : null,
                stats.rank().name(),
                stats.highestRank().name(),
                stats.totalMatches(),
                stats.victories(),
                stats.defeats(),
                stats.matchesThisWeek()
        );
    }
}