package com.f2cg.application;

import com.f2cg.domain.card.Card;
import com.f2cg.domain.deck.Deck;
import com.f2cg.domain.deck.DeckStatus;
import com.f2cg.domain.deck.DeckTheme;
import com.f2cg.infrastructure.r2dbc.CardEntityMapper;
import com.f2cg.infrastructure.r2dbc.CardRepository;
import com.f2cg.infrastructure.r2dbc.DeckEntity;
import com.f2cg.infrastructure.r2dbc.DeckRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class DeckService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final CardEntityMapper cardEntityMapper;

    public DeckService(DeckRepository deckRepository, CardRepository cardRepository,
                       CardEntityMapper cardEntityMapper) {
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.cardEntityMapper = cardEntityMapper;
    }

    public Flux<Card> getCardsByTheme(String theme) {
        return cardRepository.findByTheme(theme)
                .map(cardEntityMapper::toDomain);
    }

    public Flux<Deck> getDecksForPlayer(String playerId) {
        return deckRepository.findByPlayerId(playerId)
                .map(this::toDomain);
    }

    public Mono<DeckWithCards> getDeckWithCards(String deckId, String playerId) {
        return deckRepository.findById(deckId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found")))
                .flatMap(entity -> {
                    if (!entity.getPlayerId().equals(playerId)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
                    }
                    Deck deck = toDomain(entity);
                    List<String> cardIds = deck.cardIds();
                    if (cardIds.isEmpty()) {
                        return Mono.just(new DeckWithCards(deck, List.of()));
                    }
                    return cardRepository.findAllById(cardIds)
                            .map(cardEntityMapper::toDomain)
                            .collectList()
                            .map(cards -> new DeckWithCards(deck, cards));
                });
    }

    public Mono<Deck> createDeck(String playerId, String name, String themeStr, List<String> cardIds) {
        List<String> ids = cardIds != null ? cardIds : List.of();

        if (new HashSet<>(ids).size() != ids.size()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate card IDs"));
        }
        if (ids.size() > 20) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card limit is 20"));
        }

        DeckTheme theme;
        try {
            theme = DeckTheme.valueOf(themeStr);
        } catch (IllegalArgumentException e) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid theme"));
        }
        final DeckTheme finalTheme = theme;

        Mono<List<com.f2cg.infrastructure.r2dbc.CardEntity>> cardsMono = ids.isEmpty()
                ? Mono.just(List.of())
                : cardRepository.findAllById(ids).collectList();

        return deckRepository.countByPlayerId(playerId)
                .flatMap(count -> {
                    if (count >= 7) {
                        return Mono.<Deck>error(
                                new ResponseStatusException(HttpStatus.CONFLICT, "Deck limit reached"));
                    }
                    return cardsMono.flatMap(cards -> {
                        if (!ids.isEmpty()) {
                            if (cards.size() != ids.size()) {
                                return Mono.error(
                                        new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid card IDs"));
                            }
                            boolean wrongTheme = cards.stream()
                                    .anyMatch(c -> !c.getTheme().equals(finalTheme.name()));
                            if (wrongTheme) {
                                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Card does not belong to declared theme"));
                            }
                        }
                        DeckStatus status = ids.size() == 20 ? DeckStatus.PLAYABLE : DeckStatus.DRAFT;
                        LocalDateTime now = LocalDateTime.now();
                        DeckEntity entity = new DeckEntity(
                                UUID.randomUUID().toString(), playerId, name,
                                finalTheme.name(), CardIdConverter.toString(ids),
                                status.name(), now, now);
                        return deckRepository.save(entity).map(this::toDomain);
                    });
                });
    }

    public Mono<Deck> updateDeck(String deckId, String playerId, String name,
                                 String themeStr, List<String> cardIds) {
        List<String> ids = cardIds != null ? cardIds : List.of();

        if (new HashSet<>(ids).size() != ids.size()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate card IDs"));
        }
        if (ids.size() > 20) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card limit is 20"));
        }

        DeckTheme newTheme;
        try {
            newTheme = DeckTheme.valueOf(themeStr);
        } catch (IllegalArgumentException e) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid theme"));
        }
        final DeckTheme finalTheme = newTheme;

        return deckRepository.findById(deckId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found")))
                .flatMap(entity -> {
                    if (!entity.getPlayerId().equals(playerId)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
                    }
                    DeckTheme currentTheme = DeckTheme.valueOf(entity.getTheme());
                    boolean themeChanged = !currentTheme.equals(finalTheme);
                    if (themeChanged && !ids.isEmpty()) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Cannot change theme with cards selected"));
                    }
                    if (ids.isEmpty()) {
                        return saveUpdated(entity, name, finalTheme, ids);
                    }
                    return cardRepository.findAllById(ids).collectList()
                            .flatMap(cards -> {
                                if (cards.size() != ids.size()) {
                                    return Mono.error(new ResponseStatusException(
                                            HttpStatus.BAD_REQUEST, "Invalid card IDs"));
                                }
                                boolean wrongTheme = cards.stream()
                                        .anyMatch(c -> !c.getTheme().equals(finalTheme.name()));
                                if (wrongTheme) {
                                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                            "Card does not belong to declared theme"));
                                }
                                return saveUpdated(entity, name, finalTheme, ids);
                            });
                });
    }

    public Mono<Void> deleteDeck(String deckId, String playerId) {
        return deckRepository.findById(deckId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found")))
                .flatMap(entity -> {
                    if (!entity.getPlayerId().equals(playerId)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
                    }
                    return deckRepository.delete(entity);
                });
    }

    private Mono<Deck> saveUpdated(DeckEntity entity, String name, DeckTheme theme, List<String> ids) {
        DeckStatus status = ids.size() == 20 ? DeckStatus.PLAYABLE : DeckStatus.DRAFT;
        entity.setName(name);
        entity.setTheme(theme.name());
        entity.setCardIds(CardIdConverter.toString(ids));
        entity.setStatus(status.name());
        entity.setUpdatedAt(LocalDateTime.now());
        return deckRepository.save(entity).map(this::toDomain);
    }

    private Deck toDomain(DeckEntity entity) {
        return new Deck(
                entity.getId(),
                entity.getPlayerId(),
                entity.getName(),
                DeckTheme.valueOf(entity.getTheme()),
                CardIdConverter.toList(entity.getCardIds()),
                DeckStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}