package com.f2cg.api.dto;

import com.f2cg.application.DeckWithCards;

import java.util.List;

public record DeckWithCardsResponse(DeckResponse deck, List<CardResponse> cards) {
    public static DeckWithCardsResponse from(DeckWithCards dwc) {
        return new DeckWithCardsResponse(
                DeckResponse.from(dwc.deck()),
                dwc.cards().stream().map(CardResponse::from).toList());
    }
}