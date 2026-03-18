package com.f2cg.api.dto;

import com.f2cg.domain.deck.Deck;

import java.time.LocalDateTime;
import java.util.List;

public record DeckResponse(
        String id,
        String playerId,
        String name,
        String theme,
        List<String> cardIds,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DeckResponse from(Deck deck) {
        return new DeckResponse(
                deck.id(), deck.playerId(), deck.name(), deck.theme().name(),
                deck.cardIds(), deck.status().name(), deck.createdAt(), deck.updatedAt());
    }
}