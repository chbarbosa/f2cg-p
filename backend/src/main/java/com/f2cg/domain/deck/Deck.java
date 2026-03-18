package com.f2cg.domain.deck;

import java.time.LocalDateTime;
import java.util.List;

public record Deck(
        String id,
        String playerId,
        String name,
        DeckTheme theme,
        List<String> cardIds,
        DeckStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}