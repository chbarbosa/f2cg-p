package com.f2cg.domain.queue;

import com.f2cg.domain.season.PlayerRank;

import java.time.LocalDateTime;

public record QueueEntry(
        String id,
        String playerId,
        String deckId,
        PlayerRank matchmakingRank,
        QueueStatus status,
        LocalDateTime joinedAt
) {}