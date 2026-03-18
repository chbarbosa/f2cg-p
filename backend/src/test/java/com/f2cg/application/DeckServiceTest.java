package com.f2cg.application;

import com.f2cg.domain.card.UnitCard;
import com.f2cg.domain.card.UnitClass;
import com.f2cg.domain.deck.DeckStatus;
import com.f2cg.domain.deck.DeckTheme;
import com.f2cg.infrastructure.r2dbc.CardEntity;
import com.f2cg.infrastructure.r2dbc.CardEntityMapper;
import com.f2cg.infrastructure.r2dbc.CardRepository;
import com.f2cg.infrastructure.r2dbc.DeckEntity;
import com.f2cg.infrastructure.r2dbc.DeckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardEntityMapper cardEntityMapper;

    private DeckService deckService;

    private static final String PLAYER_ID = "player-1";
    private static final String DECK_ID = "deck-1";

    @BeforeEach
    void setUp() {
        deckService = new DeckService(deckRepository, cardRepository, cardEntityMapper);
    }

    // --- createDeck ---

    @Test
    void createDeck_noCards_returnsDraft() {
        when(deckRepository.countByPlayerId(PLAYER_ID)).thenReturn(Mono.just(0L));
        when(deckRepository.save(any(DeckEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(deckService.createDeck(PLAYER_ID, "My Deck", "WARRIOR", List.of()))
                .assertNext(deck -> {
                    assertThat(deck.status()).isEqualTo(DeckStatus.DRAFT);
                    assertThat(deck.theme()).isEqualTo(DeckTheme.WARRIOR);
                    assertThat(deck.cardIds()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void createDeck_twentyCards_returnsPlayable() {
        List<String> cardIds = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> "w-u-" + String.format("%02d", i))
                .toList();
        List<CardEntity> cardEntities = cardIds.stream()
                .map(id -> cardEntity(id, "WARRIOR"))
                .toList();

        when(deckRepository.countByPlayerId(PLAYER_ID)).thenReturn(Mono.just(0L));
        when(cardRepository.findAllById(cardIds)).thenReturn(Flux.fromIterable(cardEntities));
        when(deckRepository.save(any(DeckEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(deckService.createDeck(PLAYER_ID, "Full Deck", "WARRIOR", cardIds))
                .assertNext(deck -> assertThat(deck.status()).isEqualTo(DeckStatus.PLAYABLE))
                .verifyComplete();
    }

    @Test
    void createDeck_deckLimitReached_returns409() {
        when(deckRepository.countByPlayerId(PLAYER_ID)).thenReturn(Mono.just(7L));

        StepVerifier.create(deckService.createDeck(PLAYER_ID, "Deck", "WARRIOR", List.of()))
                .expectErrorMatches(ex ->
                        ex instanceof ResponseStatusException rse &&
                        rse.getStatusCode() == HttpStatus.CONFLICT)
                .verify();
    }

    @Test
    void createDeck_duplicateCardIds_returns400() {
        StepVerifier.create(
                deckService.createDeck(PLAYER_ID, "Deck", "WARRIOR", List.of("w-u-01", "w-u-01")))
                .expectErrorMatches(ex ->
                        ex instanceof ResponseStatusException rse &&
                        rse.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    void createDeck_moreThan20Cards_returns400() {
        List<String> ids = IntStream.rangeClosed(1, 21)
                .mapToObj(i -> "id-" + i).toList();

        StepVerifier.create(deckService.createDeck(PLAYER_ID, "Deck", "WARRIOR", ids))
                .expectErrorMatches(ex ->
                        ex instanceof ResponseStatusException rse &&
                        rse.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    void createDeck_cardFromWrongTheme_returns400() {
        String mageCardId = "m-u-01";
        CardEntity mageCard = cardEntity(mageCardId, "MAGE");

        when(deckRepository.countByPlayerId(PLAYER_ID)).thenReturn(Mono.just(0L));
        when(cardRepository.findAllById(List.of(mageCardId))).thenReturn(Flux.just(mageCard));

        StepVerifier.create(deckService.createDeck(PLAYER_ID, "Deck", "WARRIOR", List.of(mageCardId)))
                .expectErrorMatches(ex ->
                        ex instanceof ResponseStatusException rse &&
                        rse.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    // --- updateDeck ---

    @Test
    void updateDeck_themeChangeWithCards_returns400() {
        DeckEntity existing = deckEntity(DECK_ID, PLAYER_ID, "WARRIOR", "w-u-01");

        when(deckRepository.findById(DECK_ID)).thenReturn(Mono.just(existing));

        StepVerifier.create(
                deckService.updateDeck(DECK_ID, PLAYER_ID, "Deck", "MAGE", List.of("m-u-01")))
                .expectErrorMatches(ex ->
                        ex instanceof ResponseStatusException rse &&
                        rse.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    void updateDeck_notOwner_returns403() {
        DeckEntity existing = deckEntity(DECK_ID, "other-player", "WARRIOR", "");

        when(deckRepository.findById(DECK_ID)).thenReturn(Mono.just(existing));

        StepVerifier.create(
                deckService.updateDeck(DECK_ID, PLAYER_ID, "Deck", "WARRIOR", List.of()))
                .expectErrorMatches(ex ->
                        ex instanceof ResponseStatusException rse &&
                        rse.getStatusCode() == HttpStatus.FORBIDDEN)
                .verify();
    }

    // --- deleteDeck ---

    @Test
    void deleteDeck_notOwner_returns403() {
        DeckEntity existing = deckEntity(DECK_ID, "other-player", "WARRIOR", "");

        when(deckRepository.findById(DECK_ID)).thenReturn(Mono.just(existing));

        StepVerifier.create(deckService.deleteDeck(DECK_ID, PLAYER_ID))
                .expectErrorMatches(ex ->
                        ex instanceof ResponseStatusException rse &&
                        rse.getStatusCode() == HttpStatus.FORBIDDEN)
                .verify();
    }

    // --- getCardsByTheme ---

    @Test
    void getCardsByTheme_returnsFluxOfCards() {
        CardEntity e1 = cardEntity("w-u-01", "WARRIOR");
        CardEntity e2 = cardEntity("w-u-02", "WARRIOR");
        UnitCard domainCard = new UnitCard("w-u-01", "Iron Knight", 3,
                UnitClass.WARRIOR, DeckTheme.WARRIOR, 4, 5, List.of());

        when(cardRepository.findByTheme("WARRIOR")).thenReturn(Flux.just(e1, e2));
        when(cardEntityMapper.toDomain(any())).thenReturn(domainCard);

        StepVerifier.create(deckService.getCardsByTheme("WARRIOR"))
                .expectNextCount(2)
                .verifyComplete();
    }

    // --- helpers ---

    private CardEntity cardEntity(String id, String theme) {
        CardEntity e = new CardEntity();
        e.setId(id);
        e.setName("Card " + id);
        e.setManaCost(2);
        e.setCardType("UNIT");
        e.setTheme(theme);
        e.setUnitClass(theme);
        e.setAttack(3);
        e.setDefense(3);
        return e;
    }

    private DeckEntity deckEntity(String id, String playerId, String theme, String cardIds) {
        LocalDateTime now = LocalDateTime.now();
        return new DeckEntity(id, playerId, "Test Deck", theme, cardIds, "DRAFT", now, now);
    }
}
