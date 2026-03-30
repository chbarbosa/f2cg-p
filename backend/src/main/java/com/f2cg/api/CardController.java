package com.f2cg.api;

import com.f2cg.api.dto.CardResponse;
import com.f2cg.application.DeckService;
import com.f2cg.infrastructure.JwtUtil;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final DeckService deckService;
    private final JwtUtil jwtUtil;

    public CardController(DeckService deckService, JwtUtil jwtUtil) {
        this.deckService = deckService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public Flux<CardResponse> getCards(
            @RequestParam String theme,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        jwtUtil.extractPlayerIdFromHeader(auth);
        return deckService.getCardsByTheme(theme).map(CardResponse::from);
    }
}