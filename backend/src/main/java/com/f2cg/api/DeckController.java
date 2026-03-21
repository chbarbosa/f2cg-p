package com.f2cg.api;

import com.f2cg.api.dto.CreateDeckRequest;
import com.f2cg.api.dto.DeckResponse;
import com.f2cg.api.dto.DeckWithCardsResponse;
import com.f2cg.api.dto.UpdateDeckRequest;
import com.f2cg.application.DeckService;
import com.f2cg.infrastructure.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService deckService;
    private final JwtUtil jwtUtil;

    public DeckController(DeckService deckService, JwtUtil jwtUtil) {
        this.deckService = deckService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public Flux<DeckResponse> listDecks(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String playerId = jwtUtil.extractPlayerIdFromHeader(auth);
        return deckService.getDecksForPlayer(playerId).map(DeckResponse::from);
    }

    @GetMapping("/{id}")
    public Mono<DeckWithCardsResponse> getDeck(@PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String playerId = jwtUtil.extractPlayerIdFromHeader(auth);
        return deckService.getDeckWithCards(id, playerId).map(DeckWithCardsResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<DeckResponse> createDeck(@RequestBody CreateDeckRequest req,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String playerId = jwtUtil.extractPlayerIdFromHeader(auth);
        return deckService.createDeck(playerId, req.name(), req.theme(), req.cardIds())
                .map(DeckResponse::from);
    }

    @PutMapping("/{id}")
    public Mono<DeckResponse> updateDeck(@PathVariable String id,
            @RequestBody UpdateDeckRequest req,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String playerId = jwtUtil.extractPlayerIdFromHeader(auth);
        return deckService.updateDeck(id, playerId, req.name(), req.theme(), req.cardIds())
                .map(DeckResponse::from);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteDeck(@PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String playerId = jwtUtil.extractPlayerIdFromHeader(auth);
        return deckService.deleteDeck(id, playerId);
    }

}