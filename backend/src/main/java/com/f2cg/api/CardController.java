package com.f2cg.api;

import com.f2cg.api.dto.CardResponse;
import com.f2cg.application.DeckService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final DeckService deckService;

    public CardController(DeckService deckService) {
        this.deckService = deckService;
    }

    @GetMapping
    public Flux<CardResponse> getCards(@RequestParam String theme) {
        return deckService.getCardsByTheme(theme).map(CardResponse::from);
    }
}