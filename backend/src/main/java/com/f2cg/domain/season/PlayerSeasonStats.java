package com.f2cg.domain.season;

import java.time.LocalDateTime;

public record PlayerSeasonStats(
        String id,
        String playerId,
        String seasonId,
        int totalMatches,
        int victories,
        int defeats,
        PlayerRank rank,
        PlayerRank highestRank,
        int matchesThisWeek,
        LocalDateTime lastRankUpdate
) {}