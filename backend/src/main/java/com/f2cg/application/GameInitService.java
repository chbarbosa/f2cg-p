package com.f2cg.application;

import com.f2cg.infrastructure.r2dbc.CardEntityMapper;
import com.f2cg.infrastructure.r2dbc.CardRepository;
import com.f2cg.infrastructure.r2dbc.DeckRepository;
import com.f2cg.infrastructure.r2dbc.GameEntity;
import com.f2cg.infrastructure.r2dbc.GameRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GameInitService {

    private static final int HAND_SIZE = 5;

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final CardEntityMapper cardEntityMapper;
    private final GameRepository gameRepository;

    public GameInitService(DeckRepository deckRepository,
                           CardRepository cardRepository,
                           CardEntityMapper cardEntityMapper,
                           GameRepository gameRepository) {
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.cardEntityMapper = cardEntityMapper;
        this.gameRepository = gameRepository;
    }

    public Mono<Void> initializeGame(GameEntity savedGame,
                                     String player1DeckId,
                                     String player2DeckId) {
        Mono<List<String>> deck1CardIds = fetchShuffledCardIds(player1DeckId);
        Mono<List<String>> deck2CardIds = fetchShuffledCardIds(player2DeckId);

        return Mono.zip(deck1CardIds, deck2CardIds)
                .flatMap(tuple -> {
                    List<String> p1Cards = tuple.getT1();
                    List<String> p2Cards = tuple.getT2();

                    String p1Hand = CardIdConverter.toString(p1Cards.subList(0, HAND_SIZE));
                    String p1Stack = CardIdConverter.toString(p1Cards.subList(HAND_SIZE, p1Cards.size()));
                    String p2Hand = CardIdConverter.toString(p2Cards.subList(0, HAND_SIZE));
                    String p2Stack = CardIdConverter.toString(p2Cards.subList(HAND_SIZE, p2Cards.size()));

                    return gameRepository.updateGameState(
                            savedGame.getPublicId(), p1Hand, p1Stack, p2Hand, p2Stack);
                })
                .then();
    }

    private Mono<List<String>> fetchShuffledCardIds(String deckId) {
        return deckRepository.findById(deckId)
                .flatMap(deck -> {
                    List<String> cardIds = CardIdConverter.toList(deck.getCardIds());
                    return cardRepository.findAllById(cardIds)
                            .map(cardEntityMapper::toDomain)
                            .collectList();
                })
                .map(cards -> {
                    List<String> ids = new ArrayList<>();
                    for (var card : cards) {
                        ids.add(card.id());
                    }
                    Collections.shuffle(ids, new SecureRandom());
                    return ids;
                });
    }
}