package com.f2cg.application;

import com.f2cg.domain.card.Card;
import com.f2cg.domain.card.UnitCard;
import com.f2cg.domain.card.UnitClass;
import com.f2cg.domain.deck.DeckTheme;
import com.f2cg.infrastructure.r2dbc.CardEntity;
import com.f2cg.infrastructure.r2dbc.CardEntityMapper;
import com.f2cg.infrastructure.r2dbc.CardRepository;
import com.f2cg.infrastructure.r2dbc.DeckEntity;
import com.f2cg.infrastructure.r2dbc.DeckRepository;
import com.f2cg.infrastructure.r2dbc.GameEntity;
import com.f2cg.infrastructure.r2dbc.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameInitServiceTest {

    @Mock DeckRepository deckRepository;
    @Mock CardRepository cardRepository;
    @Mock CardEntityMapper cardEntityMapper;
    @Mock GameRepository gameRepository;

    GameInitService gameInitService;

    static final String GAME_PUBLIC_ID = "game-pub-id";
    static final String DECK1_ID = "deck1-id";
    static final String DECK2_ID = "deck2-id";

    @BeforeEach
    void setUp() {
        gameInitService = new GameInitService(deckRepository, cardRepository, cardEntityMapper, gameRepository);
        lenient().when(gameRepository.updateGameState(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(1));
    }

    private List<CardEntity> buildCardEntities(int count) {
        List<CardEntity> entities = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            CardEntity e = new CardEntity();
            e.setId(String.format("card-%02d", i));
            e.setName("Card " + i);
            e.setManaCost(1);
            e.setCardType("UNIT");
            e.setTheme("WARRIOR");
            e.setUnitClass("WARRIOR");
            e.setAttack(1);
            e.setDefense(1);
            entities.add(e);
        }
        return entities;
    }

    private Card buildCard(String id) {
        return new UnitCard(id, "Card " + id, 1, UnitClass.WARRIOR, DeckTheme.WARRIOR, 1, 1, List.of());
    }

    private GameEntity buildGameEntity() {
        return new GameEntity(GAME_PUBLIC_ID, "p1", "p1", "p1name", "p2", "p2", "p2name", "WAITING_START", LocalDateTime.now());
    }

    private void setupDeckAndCards(String deckId, List<CardEntity> cardEntities) {
        DeckEntity deck = new DeckEntity(deckId, "player", "MyDeck", "WARRIOR",
                cardEntities.stream().map(CardEntity::getId).reduce((a, b) -> a + "," + b).orElse(""),
                "PLAYABLE", LocalDateTime.now(), LocalDateTime.now());
        lenient().when(deckRepository.findById(deckId)).thenReturn(Mono.just(deck));

        List<String> ids = cardEntities.stream().map(CardEntity::getId).toList();
        lenient().when(cardRepository.findAllById(ids)).thenReturn(Flux.fromIterable(cardEntities));

        for (CardEntity e : cardEntities) {
            lenient().when(cardEntityMapper.toDomain(e)).thenReturn(buildCard(e.getId()));
        }
    }

    @Test
    void initializeGame_dealsExactly5CardsToEachHand() {
        List<CardEntity> deck1 = buildCardEntities(40);
        List<CardEntity> deck2 = buildCardEntities(40);
        setupDeckAndCards(DECK1_ID, deck1);
        setupDeckAndCards(DECK2_ID, deck2);

        StepVerifier.create(gameInitService.initializeGame(buildGameEntity(), DECK1_ID, DECK2_ID))
                .verifyComplete();

        ArgumentCaptor<String> p1HandCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> p1StackCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> p2HandCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> p2StackCaptor = ArgumentCaptor.forClass(String.class);

        verify(gameRepository).updateGameState(
                eq(GAME_PUBLIC_ID),
                p1HandCaptor.capture(), p1StackCaptor.capture(),
                p2HandCaptor.capture(), p2StackCaptor.capture()
        );

        assertThat(CardIdConverter.toList(p1HandCaptor.getValue())).hasSize(5);
        assertThat(CardIdConverter.toList(p2HandCaptor.getValue())).hasSize(5);
    }

    @Test
    void initializeGame_puts35CardsInEachStack() {
        List<CardEntity> deck1 = buildCardEntities(40);
        List<CardEntity> deck2 = buildCardEntities(40);
        setupDeckAndCards(DECK1_ID, deck1);
        setupDeckAndCards(DECK2_ID, deck2);

        StepVerifier.create(gameInitService.initializeGame(buildGameEntity(), DECK1_ID, DECK2_ID))
                .verifyComplete();

        ArgumentCaptor<String> p1StackCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> p2StackCaptor = ArgumentCaptor.forClass(String.class);

        verify(gameRepository).updateGameState(
                eq(GAME_PUBLIC_ID),
                anyString(), p1StackCaptor.capture(),
                anyString(), p2StackCaptor.capture()
        );

        assertThat(CardIdConverter.toList(p1StackCaptor.getValue())).hasSize(35);
        assertThat(CardIdConverter.toList(p2StackCaptor.getValue())).hasSize(35);
    }

    @Test
    void initializeGame_handAndStackContainAllDeckCards_noMissingNoDuplicates() {
        List<CardEntity> deck1 = buildCardEntities(40);
        List<CardEntity> deck2 = buildCardEntities(40);
        setupDeckAndCards(DECK1_ID, deck1);
        setupDeckAndCards(DECK2_ID, deck2);

        StepVerifier.create(gameInitService.initializeGame(buildGameEntity(), DECK1_ID, DECK2_ID))
                .verifyComplete();

        ArgumentCaptor<String> p1HandCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> p1StackCaptor = ArgumentCaptor.forClass(String.class);

        verify(gameRepository).updateGameState(
                eq(GAME_PUBLIC_ID),
                p1HandCaptor.capture(), p1StackCaptor.capture(),
                anyString(), anyString()
        );

        List<String> allP1 = new ArrayList<>(CardIdConverter.toList(p1HandCaptor.getValue()));
        allP1.addAll(CardIdConverter.toList(p1StackCaptor.getValue()));

        Set<String> originalIds = new HashSet<>(deck1.stream().map(CardEntity::getId).toList());
        assertThat(allP1).hasSize(40);
        assertThat(new HashSet<>(allP1)).isEqualTo(originalIds);
    }

    @Test
    void initializeGame_shuffleProducesDifferentOrderAtLeastOnceIn10Runs() {
        // Use fixed deck entities for all runs — same stubs, called multiple times
        List<CardEntity> deck1Entities = buildCardEntities(40);
        List<CardEntity> deck2Entities = buildCardEntities(40);
        setupDeckAndCards(DECK1_ID, deck1Entities);
        setupDeckAndCards(DECK2_ID, deck2Entities);

        List<String> originalOrder = deck1Entities.stream().map(CardEntity::getId).toList();

        ArgumentCaptor<String> p1HandCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> p1StackCaptor = ArgumentCaptor.forClass(String.class);

        boolean foundDifferentOrder = false;
        for (int run = 0; run < 10; run++) {
            StepVerifier.create(gameInitService.initializeGame(buildGameEntity(), DECK1_ID, DECK2_ID))
                    .verifyComplete();
        }

        // Capture all 10 invocations
        verify(gameRepository, org.mockito.Mockito.times(10)).updateGameState(
                anyString(),
                p1HandCaptor.capture(), p1StackCaptor.capture(),
                anyString(), anyString()
        );

        List<String> p1Hands = p1HandCaptor.getAllValues();
        List<String> p1Stacks = p1StackCaptor.getAllValues();

        for (int i = 0; i < 10; i++) {
            List<String> combined = new ArrayList<>(CardIdConverter.toList(p1Hands.get(i)));
            combined.addAll(CardIdConverter.toList(p1Stacks.get(i)));
            if (!combined.equals(originalOrder)) {
                foundDifferentOrder = true;
                break;
            }
        }

        assertThat(foundDifferentOrder)
                .as("Shuffling 40 cards 10 times should produce at least one different order")
                .isTrue();
    }

    @Test
    void initializeGame_callsUpdateGameStateWithPublicId() {
        List<CardEntity> deck1 = buildCardEntities(40);
        List<CardEntity> deck2 = buildCardEntities(40);
        setupDeckAndCards(DECK1_ID, deck1);
        setupDeckAndCards(DECK2_ID, deck2);

        StepVerifier.create(gameInitService.initializeGame(buildGameEntity(), DECK1_ID, DECK2_ID))
                .verifyComplete();

        verify(gameRepository).updateGameState(
                eq(GAME_PUBLIC_ID), anyString(), anyString(), anyString(), anyString()
        );
    }
}