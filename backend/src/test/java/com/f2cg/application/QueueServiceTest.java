package com.f2cg.application;

import com.f2cg.domain.player.Player;
import com.f2cg.domain.queue.QueueStatus;
import com.f2cg.domain.season.PlayerRank;
import com.f2cg.domain.season.Season;
import com.f2cg.domain.season.SeasonPhase;
import com.f2cg.domain.season.SeasonStatus;
import com.f2cg.infrastructure.r2dbc.DeckEntity;
import com.f2cg.infrastructure.r2dbc.DeckRepository;
import com.f2cg.infrastructure.r2dbc.GameEntity;
import com.f2cg.infrastructure.r2dbc.GameRepository;
import com.f2cg.infrastructure.r2dbc.PlayerRepository;
import com.f2cg.infrastructure.r2dbc.PlayerSeasonStatsEntity;
import com.f2cg.infrastructure.r2dbc.PlayerSeasonStatsRepository;
import com.f2cg.infrastructure.r2dbc.QueueEntryEntity;
import com.f2cg.infrastructure.r2dbc.QueueEntryRepository;
import com.f2cg.infrastructure.sse.QueueSseBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    @Mock private QueueEntryRepository queueEntryRepository;
    @Mock private DeckRepository deckRepository;
    @Mock private SeasonService seasonService;
    @Mock private RankCalculationService rankCalculationService;
    @Mock private PlayerSeasonStatsRepository playerSeasonStatsRepository;
    @Mock private GameRepository gameRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private QueueSseBroadcaster sseBroadcaster;

    private QueueService queueService;

    private static final String PLAYER_ID   = "player-1";
    private static final String OPPONENT_ID = "player-opp";
    private static final String DECK_ID     = "deck-1";
    private static final String SEASON_ID   = "season-1";

    @BeforeEach
    void setUp() {
        queueService = new QueueService(
                queueEntryRepository, deckRepository,
                seasonService, rankCalculationService, playerSeasonStatsRepository,
                gameRepository, playerRepository, sseBroadcaster);
        ReflectionTestUtils.setField(queueService, "timeoutSeconds", 30);

        // Lenient stubs for matchmaking methods — not used by all tests
        lenient().when(queueEntryRepository.findFirstEligibleOpponentNullRank(any()))
                .thenReturn(Mono.empty());
        lenient().when(queueEntryRepository.findFirstEligibleOpponentWithRank(any(), any()))
                .thenReturn(Mono.empty());
        lenient().when(playerRepository.findById(PLAYER_ID))
                .thenReturn(Mono.just(player(PLAYER_ID, "Player One")));
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
        when(queueEntryRepository.findFirstEligibleOpponentWithRank(PLAYER_ID, "ELITE"))
                .thenReturn(Mono.empty());

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
                .thenReturn(Mono.empty());
        when(rankCalculationService.getMatchmakingRank(PlayerRank.PENDING, SeasonPhase.RANKED))
                .thenReturn(PlayerRank.ROOKIE);
        when(queueEntryRepository.findByPlayerIdAndStatus(PLAYER_ID, "WAITING"))
                .thenReturn(Mono.empty());
        when(queueEntryRepository.save(any(QueueEntryEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(queueEntryRepository.findFirstEligibleOpponentWithRank(PLAYER_ID, "ROOKIE"))
                .thenReturn(Mono.empty());

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
        when(queueEntryRepository.findFirstEligibleOpponentWithRank(PLAYER_ID, "ROOKIE"))
                .thenReturn(Mono.empty());

        StepVerifier.create(queueService.joinQueue(PLAYER_ID, DECK_ID))
                .expectNextCount(1)
                .verifyComplete();

        verify(rankCalculationService).getMatchmakingRank(eq(PlayerRank.ROOKIE), eq(SeasonPhase.RANKED));
    }

    // --- Matchmaking ---

    @Test
    void joinQueue_matchFound_whenEligibleOpponentWaiting() {
        QueueEntryEntity opponent = new QueueEntryEntity(
                "entry-opp", OPPONENT_ID, "deck-opp", "WAITING", null,
                LocalDateTime.now().minusSeconds(10));
        Player p1 = player(PLAYER_ID, "Player One");
        Player p2 = player(OPPONENT_ID, "Opponent");

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
        when(queueEntryRepository.findFirstEligibleOpponentNullRank(PLAYER_ID))
                .thenReturn(Mono.just(opponent));
        when(queueEntryRepository.claimForMatch("entry-opp")).thenReturn(Mono.just(1));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Mono.just(p1));
        when(playerRepository.findById(OPPONENT_ID)).thenReturn(Mono.just(p2));
        when(gameRepository.save(any(GameEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(queueService.joinQueue(PLAYER_ID, DECK_ID))
                .assertNext(entry -> assertThat(entry.status()).isEqualTo(QueueStatus.MATCHED))
                .verifyComplete();

        verify(gameRepository).save(any(GameEntity.class));
        verify(sseBroadcaster).emit(eq(PLAYER_ID), eq("MATCH_FOUND"), any());
        verify(sseBroadcaster).emit(eq(OPPONENT_ID), eq("MATCH_FOUND"), any());
    }

    @Test
    void joinQueue_noMatch_whenNoOpponentInQueue() {
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
        // No opponent found (default from setUp lenient stub)

        StepVerifier.create(queueService.joinQueue(PLAYER_ID, DECK_ID))
                .assertNext(entry -> assertThat(entry.status()).isEqualTo(QueueStatus.WAITING))
                .verifyComplete();

        verify(gameRepository, never()).save(any());
        verify(sseBroadcaster, never()).emit(any(), any(), any());
    }

    @Test
    void joinQueue_noMatch_whenOpponentHasDifferentRankInRanked() {
        when(deckRepository.findById(DECK_ID)).thenReturn(Mono.just(playableDeck()));
        when(seasonService.getCurrentSeason()).thenReturn(Mono.just(rankedSeason()));
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.RANKED);
        when(playerSeasonStatsRepository.findByPlayerIdAndSeasonId(PLAYER_ID, SEASON_ID))
                .thenReturn(Mono.empty());
        when(rankCalculationService.getMatchmakingRank(PlayerRank.PENDING, SeasonPhase.RANKED))
                .thenReturn(PlayerRank.ROOKIE);
        when(queueEntryRepository.findByPlayerIdAndStatus(PLAYER_ID, "WAITING"))
                .thenReturn(Mono.empty());
        when(queueEntryRepository.save(any(QueueEntryEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        // Only ROOKIE opponents searched — none found (lenient stub returns Mono.empty())
        when(queueEntryRepository.findFirstEligibleOpponentWithRank(PLAYER_ID, "ROOKIE"))
                .thenReturn(Mono.empty());

        StepVerifier.create(queueService.joinQueue(PLAYER_ID, DECK_ID))
                .assertNext(entry -> assertThat(entry.status()).isEqualTo(QueueStatus.WAITING))
                .verifyComplete();

        verify(gameRepository, never()).save(any());
    }

    @Test
    void joinQueue_oldestOpponentSelected_queryOrderedByJoinedAt() {
        // The repository query has ORDER BY joined_at ASC LIMIT 1, so the service
        // always calls findFirstEligibleOpponent* which returns the oldest opponent.
        // This test verifies that the service uses the repository result directly
        // (first result = oldest, since the DB handles ordering).
        QueueEntryEntity oldest = new QueueEntryEntity(
                "entry-oldest", OPPONENT_ID, "deck-opp", "WAITING", null,
                LocalDateTime.now().minusMinutes(5));
        Player p1 = player(PLAYER_ID, "Player One");
        Player p2 = player(OPPONENT_ID, "Oldest Opponent");

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
        when(queueEntryRepository.findFirstEligibleOpponentNullRank(PLAYER_ID))
                .thenReturn(Mono.just(oldest));
        when(queueEntryRepository.claimForMatch("entry-oldest")).thenReturn(Mono.just(1));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Mono.just(p1));
        when(playerRepository.findById(OPPONENT_ID)).thenReturn(Mono.just(p2));
        when(gameRepository.save(any(GameEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(queueService.joinQueue(PLAYER_ID, DECK_ID))
                .assertNext(entry -> assertThat(entry.status()).isEqualTo(QueueStatus.MATCHED))
                .verifyComplete();

        // Verify the first eligible opponent query was used (not some other selection)
        verify(queueEntryRepository).findFirstEligibleOpponentNullRank(PLAYER_ID);
    }

    // --- checkTimeouts ---

    @Test
    void checkTimeouts_timedOutEntry_setsStatusTimedOut() {
        QueueEntryEntity stale = new QueueEntryEntity(
                "entry-stale", PLAYER_ID, DECK_ID, "WAITING", null,
                LocalDateTime.now().minusMinutes(2));

        when(queueEntryRepository.findTimedOutEntries(any(LocalDateTime.class)))
                .thenReturn(Flux.just(stale));
        when(queueEntryRepository.save(any(QueueEntryEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        queueService.checkTimeouts();

        verify(queueEntryRepository).save(any(QueueEntryEntity.class));
        verify(sseBroadcaster).emit(eq(PLAYER_ID), eq("QUEUE_TIMEOUT"), any());
        assertThat(stale.getStatus()).isEqualTo(QueueStatus.TIMED_OUT.name());
    }

    @Test
    void checkTimeouts_cutoffUsesTimeoutSecondsProperty() {
        ReflectionTestUtils.setField(queueService, "timeoutSeconds", 60);
        when(queueEntryRepository.findTimedOutEntries(any(LocalDateTime.class)))
                .thenReturn(Flux.empty());

        LocalDateTime before = LocalDateTime.now().minusSeconds(61);
        LocalDateTime after = LocalDateTime.now().minusSeconds(59);

        queueService.checkTimeouts();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(queueEntryRepository).findTimedOutEntries(captor.capture());
        assertThat(captor.getValue()).isBetween(before, after);
    }

    // --- cancelQueue ---

    @Test
    void cancelQueue_existingWaitingEntry_completes() {
        when(queueEntryRepository.cancelIfWaiting(PLAYER_ID)).thenReturn(Mono.just(1));

        StepVerifier.create(queueService.cancelQueue(PLAYER_ID))
                .verifyComplete();
    }

    @Test
    void cancelQueue_noActiveEntry_returns404() {
        when(queueEntryRepository.cancelIfWaiting(PLAYER_ID)).thenReturn(Mono.just(0));

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

    private Player player(String id, String nickname) {
        return new Player(id, id + "@test.com", "hash", true, null, null, nickname, "US");
    }

    private Season freeSeason() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        return new Season(SEASON_ID, 2026, 1, "S1 2026",
                start, LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 2, 1),
                SeasonStatus.ACTIVE, null);
    }

    private Season rankedSeason() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        return new Season(SEASON_ID, 2026, 1, "S1 2026",
                start, LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 1, 1),
                SeasonStatus.ACTIVE, null);
    }

    private PlayerSeasonStatsEntity statsEntity(String rank) {
        return new PlayerSeasonStatsEntity("stats-1", PLAYER_ID, SEASON_ID,
                10, 5, 5, rank, rank, 3, null);
    }
}