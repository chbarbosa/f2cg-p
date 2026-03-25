package com.f2cg.application;

import com.f2cg.domain.queue.QueueStatus;
import com.f2cg.domain.season.PlayerRank;
import com.f2cg.domain.season.Season;
import com.f2cg.domain.season.SeasonPhase;
import com.f2cg.domain.season.SeasonStatus;
import com.f2cg.infrastructure.r2dbc.DeckEntity;
import com.f2cg.infrastructure.r2dbc.DeckRepository;
import com.f2cg.infrastructure.r2dbc.PlayerSeasonStatsEntity;
import com.f2cg.infrastructure.r2dbc.PlayerSeasonStatsRepository;
import com.f2cg.infrastructure.r2dbc.QueueEntryEntity;
import com.f2cg.infrastructure.r2dbc.QueueEntryRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private SeasonService seasonService;

    @Mock
    private RankCalculationService rankCalculationService;

    @Mock
    private PlayerSeasonStatsRepository playerSeasonStatsRepository;

    private QueueService queueService;

    private static final String PLAYER_ID = "player-1";
    private static final String DECK_ID   = "deck-1";
    private static final String SEASON_ID = "season-1";

    @BeforeEach
    void setUp() {
        queueService = new QueueService(queueEntryRepository, deckRepository,
                seasonService, rankCalculationService, playerSeasonStatsRepository);
    }

    // --- joinQueue ---

    @Test
    void joinQueue_validPlayableDeck_returnsWaitingEntry() {
        when(deckRepository.findById(DECK_ID)).thenReturn(Mono.just(playableDeck()));
        when(seasonService.getCurrentSeason()).thenReturn(Mono.just(freeSeason()));
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.FREE);
        when(playerSeasonStatsRepository.findByPlayerIdAndSeasonId(PLAYER_ID, SEASON_ID))
                .thenReturn(Mono.empty());
        when(rankCalculationService.getMatchmakingRank(PlayerRank.PENDING, SeasonPhase.FREE))
                .thenReturn(null);
        when(queueEntryRepository.findByPlayerIdAndStatus(PLAYER_ID, "WAITING"))
                .thenReturn(Mono.empty());
        when(queueEntryRepository.save(any(QueueEntryEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(queueService.joinQueue(PLAYER_ID, DECK_ID))
                .assertNext(entry -> {
                    assertThat(entry.playerId()).isEqualTo(PLAYER_ID);
                    assertThat(entry.deckId()).isEqualTo(DECK_ID);
                    assertThat(entry.status()).isEqualTo(QueueStatus.WAITING);
                    assertThat(entry.id()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void joinQueue_deckNotFound_returns404() {
        when(deckRepository.findById(DECK_ID)).thenReturn(Mono.empty());

        StepVerifier.create(queueService.joinQueue(PLAYER_ID, DECK_ID))
                .expectErrorMatches(ex -> ex instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void joinQueue_deckOwnedByOtherPlayer_returns403() {
        DeckEntity deck = playableDeck();
        deck.setPlayerId("other-player");
        when(deckRepository.findById(DECK_ID)).thenReturn(Mono.just(deck));

        StepVerifier.create(queueService.joinQueue(PLAYER_ID, DECK_ID))
                .expectErrorMatches(ex -> ex instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.FORBIDDEN)
                .verify();
    }

    @Test
    void joinQueue_deckIsDraft_returns400() {
        DeckEntity deck = playableDeck();
        deck.setStatus("DRAFT");
        when(deckRepository.findById(DECK_ID)).thenReturn(Mono.just(deck));

        StepVerifier.create(queueService.joinQueue(PLAYER_ID, DECK_ID))
                .expectErrorMatches(ex -> ex instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    void joinQueue_playerAlreadyWaiting_returns409() {
        when(deckRepository.findById(DECK_ID)).thenReturn(Mono.just(playableDeck()));
        when(seasonService.getCurrentSeason()).thenReturn(Mono.just(freeSeason()));
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.FREE);
        when(playerSeasonStatsRepository.findByPlayerIdAndSeasonId(PLAYER_ID, SEASON_ID))
                .thenReturn(Mono.empty());
        when(rankCalculationService.getMatchmakingRank(PlayerRank.PENDING, SeasonPhase.FREE))
                .thenReturn(null);
        when(queueEntryRepository.findByPlayerIdAndStatus(PLAYER_ID, "WAITING"))
                .thenReturn(Mono.just(waitingEntry()));

        StepVerifier.create(queueService.joinQueue(PLAYER_ID, DECK_ID))
                .expectErrorMatches(ex -> ex instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.CONFLICT)
                .verify();
    }

    // --- Season & Rank integration ---

    @Test
    void joinQueue_freePhase_storesNullMatchmakingRank() {
        when(deckRepository.findById(DECK_ID)).thenReturn(Mono.just(playableDeck()));
        when(seasonService.getCurrentSeason()).thenReturn(Mono.just(freeSeason()));
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.FREE);
        when(playerSeasonStatsRepository.findByPlayerIdAndSeasonId(PLAYER_ID, SEASON_ID))
                .thenReturn(Mono.just(statsEntity("ELITE")));
        when(rankCalculationService.getMatchmakingRank(PlayerRank.ELITE, SeasonPhase.FREE))
                .thenReturn(null);
        when(queueEntryRepository.findByPlayerIdAndStatus(PLAYER_ID, "WAITING"))
                .thenReturn(Mono.empty());
        when(queueEntryRepository.save(any(QueueEntryEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(queueService.joinQueue(PLAYER_ID, DECK_ID))
                .assertNext(entry -> assertThat(entry.matchmakingRank()).isNull())
                .verifyComplete();
    }

    @Test
    void joinQueue_rankedPhase_elitePlayer_storesElite() {
        when(deckRepository.findById(DECK_ID)).thenReturn(Mono.just(playableDeck()));
        when(seasonService.getCurrentSeason()).thenReturn(Mono.just(rankedSeason()));
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.RANKED);
        when(playerSeasonStatsRepository.findByPlayerIdAndSeasonId(PLAYER_ID, SEASON_ID))
                .thenReturn(Mono.just(statsEntity("ELITE")));
        when(rankCalculationService.getMatchmakingRank(PlayerRank.ELITE, SeasonPhase.RANKED))
                .thenReturn(PlayerRank.ELITE);
        when(queueEntryRepository.findByPlayerIdAndStatus(PLAYER_ID, "WAITING"))
                .thenReturn(Mono.empty());
        when(queueEntryRepository.save(any(QueueEntryEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(queueService.joinQueue(PLAYER_ID, DECK_ID))
                .assertNext(entry -> assertThat(entry.matchmakingRank()).isEqualTo(PlayerRank.ELITE))
                .verifyComplete();
    }

    @Test
    void joinQueue_rankedPhase_pendingPlayer_storesRookie() {
        when(deckRepository.findById(DECK_ID)).thenReturn(Mono.just(playableDeck()));
        when(seasonService.getCurrentSeason()).thenReturn(Mono.just(rankedSeason()));
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.RANKED);
        when(playerSeasonStatsRepository.findByPlayerIdAndSeasonId(PLAYER_ID, SEASON_ID))
                .thenReturn(Mono.empty()); // no stats → PENDING
        when(rankCalculationService.getMatchmakingRank(PlayerRank.PENDING, SeasonPhase.RANKED))
                .thenReturn(PlayerRank.ROOKIE);
        when(queueEntryRepository.findByPlayerIdAndStatus(PLAYER_ID, "WAITING"))
                .thenReturn(Mono.empty());
        when(queueEntryRepository.save(any(QueueEntryEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(queueService.joinQueue(PLAYER_ID, DECK_ID))
                .assertNext(entry -> assertThat(entry.matchmakingRank()).isEqualTo(PlayerRank.ROOKIE))
                .verifyComplete();
    }

    @Test
    void joinQueue_callsGetCurrentSeasonOnEveryJoin() {
        when(deckRepository.findById(DECK_ID)).thenReturn(Mono.just(playableDeck()));
        when(seasonService.getCurrentSeason()).thenReturn(Mono.just(freeSeason()));
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.FREE);
        when(playerSeasonStatsRepository.findByPlayerIdAndSeasonId(PLAYER_ID, SEASON_ID))
                .thenReturn(Mono.empty());
        when(rankCalculationService.getMatchmakingRank(PlayerRank.PENDING, SeasonPhase.FREE))
                .thenReturn(null);
        when(queueEntryRepository.findByPlayerIdAndStatus(PLAYER_ID, "WAITING"))
                .thenReturn(Mono.empty());
        when(queueEntryRepository.save(any(QueueEntryEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(queueService.joinQueue(PLAYER_ID, DECK_ID))
                .expectNextCount(1)
                .verifyComplete();

        verify(seasonService).getCurrentSeason();
    }

    @Test
    void joinQueue_callsGetMatchmakingRankOnEveryJoin() {
        when(deckRepository.findById(DECK_ID)).thenReturn(Mono.just(playableDeck()));
        when(seasonService.getCurrentSeason()).thenReturn(Mono.just(rankedSeason()));
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.RANKED);
        when(playerSeasonStatsRepository.findByPlayerIdAndSeasonId(PLAYER_ID, SEASON_ID))
                .thenReturn(Mono.just(statsEntity("ROOKIE")));
        when(rankCalculationService.getMatchmakingRank(PlayerRank.ROOKIE, SeasonPhase.RANKED))
                .thenReturn(PlayerRank.ROOKIE);
        when(queueEntryRepository.findByPlayerIdAndStatus(PLAYER_ID, "WAITING"))
                .thenReturn(Mono.empty());
        when(queueEntryRepository.save(any(QueueEntryEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(queueService.joinQueue(PLAYER_ID, DECK_ID))
                .expectNextCount(1)
                .verifyComplete();

        verify(rankCalculationService).getMatchmakingRank(eq(PlayerRank.ROOKIE), eq(SeasonPhase.RANKED));
    }

    // --- cancelQueue ---

    @Test
    void cancelQueue_existingWaitingEntry_setsStatusCancelled() {
        when(queueEntryRepository.findByPlayerIdAndStatus(PLAYER_ID, "WAITING"))
                .thenReturn(Mono.just(waitingEntry()));
        when(queueEntryRepository.save(any(QueueEntryEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(queueService.cancelQueue(PLAYER_ID))
                .assertNext(entry -> assertThat(entry.status()).isEqualTo(QueueStatus.CANCELLED))
                .verifyComplete();
    }

    @Test
    void cancelQueue_noActiveEntry_returns404() {
        when(queueEntryRepository.findByPlayerIdAndStatus(PLAYER_ID, "WAITING"))
                .thenReturn(Mono.empty());

        StepVerifier.create(queueService.cancelQueue(PLAYER_ID))
                .expectErrorMatches(ex -> ex instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    // --- getStatus ---

    @Test
    void getStatus_waitingEntry_returnsIt() {
        when(queueEntryRepository.findByPlayerIdAndStatusIn(PLAYER_ID, List.of("WAITING", "MATCHED")))
                .thenReturn(Flux.just(waitingEntry()));

        StepVerifier.create(queueService.getStatus(PLAYER_ID))
                .assertNext(entry -> {
                    assertThat(entry.playerId()).isEqualTo(PLAYER_ID);
                    assertThat(entry.status()).isEqualTo(QueueStatus.WAITING);
                })
                .verifyComplete();
    }

    @Test
    void getStatus_noActiveEntry_returns404() {
        when(queueEntryRepository.findByPlayerIdAndStatusIn(PLAYER_ID, List.of("WAITING", "MATCHED")))
                .thenReturn(Flux.empty());

        StepVerifier.create(queueService.getStatus(PLAYER_ID))
                .expectErrorMatches(ex -> ex instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    // --- helpers ---

    private DeckEntity playableDeck() {
        LocalDateTime now = LocalDateTime.now();
        return new DeckEntity(DECK_ID, PLAYER_ID, "Test Deck", "WARRIOR",
                "w-u-01,w-u-02", "PLAYABLE", now, now);
    }

    private QueueEntryEntity waitingEntry() {
        return new QueueEntryEntity("entry-1", PLAYER_ID, DECK_ID, "WAITING", null, LocalDateTime.now());
    }

    private Season freeSeason() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        return new Season(SEASON_ID, 2026, 1, "S1 2026",
                start, LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 2, 1), // phase2 starts Feb 1
                SeasonStatus.ACTIVE, null);
    }

    private Season rankedSeason() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        return new Season(SEASON_ID, 2026, 1, "S1 2026",
                start, LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 1, 1), // phase2 starts Jan 1 → always RANKED
                SeasonStatus.ACTIVE, null);
    }

    private PlayerSeasonStatsEntity statsEntity(String rank) {
        return new PlayerSeasonStatsEntity("stats-1", PLAYER_ID, SEASON_ID,
                10, 5, 5, rank, rank, 3, null);
    }
}