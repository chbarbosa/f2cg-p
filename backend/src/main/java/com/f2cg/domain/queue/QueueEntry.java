package com.f2cg.domain.queue;

import java.time.LocalDateTime;

public record QueueEntry(
        String id,
        String playerId,
        String deckId,
        QueueStatus status,
        LocalDateTime joinedAt
) {}