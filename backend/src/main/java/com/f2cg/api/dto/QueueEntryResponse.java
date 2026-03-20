package com.f2cg.api.dto;

import com.f2cg.domain.queue.QueueEntry;

import java.time.LocalDateTime;

public record QueueEntryResponse(
        String id,
        String playerId,
        String deckId,
        String status,
        LocalDateTime joinedAt
) {
    public static QueueEntryResponse from(QueueEntry entry) {
        return new QueueEntryResponse(
                entry.id(),
                entry.playerId(),
                entry.deckId(),
                entry.status().name(),
                entry.joinedAt()
        );
    }
}