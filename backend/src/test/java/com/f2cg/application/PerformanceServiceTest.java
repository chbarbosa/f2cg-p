package com.f2cg.application;

import com.f2cg.domain.season.PlayerRank;
import com.f2cg.domain.season.Season;
import com.f2cg.domain.season.SeasonPhase;
import com.f2cg.domain.season.SeasonStatus;
import com.f2cg.infrastructure.r2dbc.PlayerSeasonStatsEntity;
import com.f2cg.infrastructure.r2dbc.PlayerSeasonStatsRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerformanceServiceTest {

    @Mock
    private SeasonService seasonService;
    @Mock
    private PlayerSeasonStatsRepository playerSeasonStatsRepository;

    private PerformanceService performanceService;

    private static final Season ACTIVE_SEASON = new Season(
            "s-2026-1", 2026, 1, "Season 1",
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 2, 28),
            LocalDate.of(2026, 2, 1),
            SeasonStatus.ACTIVE,
            null
    );

    private static final Season PAST_SEASON = new Season(
            "s-2025-2", 2025, 2, "Season 2",
            LocalDate.of(2025, 7, 1),
            LocalDate.of(2025, 8, 31),
            LocalDate.of(2025, 8, 1),
            SeasonStatus.FINISHED,
            null
    );

    private static PlayerSeasonStatsEntity statsEntity(String playerId, String seasonId) {
        return new PlayerSeasonStatsEntity(
                "stats-1", playerId, seasonId,
                10, 6, 4,
                PlayerRank.INTERMEDIATE.name(), PlayerRank.ADVANCED.name(),
                3, null
        );
    }

    @BeforeEach
    void setUp() {
        performanceService = new PerformanceService(seasonService, playerSeasonStatsRepository);
    }

    // --- getCurrentPerformance ---

    @Test
    void getCurrentPerformance_withExistingStats_returnsPerformanceWithPhase() {
        String playerId = "player-1";
        when(seasonService.getCurrentSeason()).thenReturn(Mono.just(ACTIVE_SEASON));
        when(playerSeasonStatsRepository.findByPlayerIdAndSeasonId(playerId, ACTIVE_SEASON.id()))
                .thenReturn(Mono.just(statsEntity(playerId, ACTIVE_SEASON.id())));
        when(seasonService.getCurrentPhase(ACTIVE_SEASON, LocalDate.now()))
                .thenReturn(SeasonPhase.FREE);

        StepVerifier.create(performanceService.getCurrentPerformance(playerId))
                .assertNext(response -> {
                    assertThat(response.season().id()).isEqualTo(ACTIVE_SEASON.id());
                    assertThat(response.rank()).isEqualTo(PlayerRank.INTERMEDIATE.name());
                    assertThat(response.highestRank()).isEqualTo(PlayerRank.ADVANCED.name());
                    assertThat(response.totalMatches()).isEqualTo(10);
                    assertThat(response.victories()).isEqualTo(6);
                    assertThat(response.defeats()).isEqualTo(4);
                    assertThat(response.matchesThisWeek()).isEqualTo(3);
                    assertThat(response.currentPhase()).isEqualTo(SeasonPhase.FREE.name());
                })
                .verifyComplete();
    }

    @Test
    void getCurrentPerformance_withNoStats_returnsDefaultStats() {
        String playerId = "player-new";
        when(seasonService.getCurrentSeason()).thenReturn(Mono.just(ACTIVE_SEASON));
        when(playerSeasonStatsRepository.findByPlayerIdAndSeasonId(playerId, ACTIVE_SEASON.id()))
                .thenReturn(Mono.empty());
        when(seasonService.getCurrentPhase(ACTIVE_SEASON, LocalDate.now()))
                .thenReturn(SeasonPhase.RANKED);

        StepVerifier.create(performanceService.getCurrentPerformance(playerId))
                .assertNext(response -> {
                    assertThat(response.rank()).isEqualTo(PlayerRank.PENDING.name());
                    assertThat(response.highestRank()).isEqualTo(PlayerRank.PENDING.name());
                    assertThat(response.totalMatches()).isZero();
                    assertThat(response.victories()).isZero();
                    assertThat(response.defeats()).isZero();
                    assertThat(response.matchesThisWeek()).isZero();
                    assertThat(response.currentPhase()).isEqualTo(SeasonPhase.RANKED.name());
                })
                .verifyComplete();
    }

    @Test
    void getCurrentPerformance_whenNoActiveSeason_propagatesError() {
        String playerId = "player-1";
        when(seasonService.getCurrentSeason())
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No active season")));

        StepVerifier.create(performanceService.getCurrentPerformance(playerId))
                .expectErrorMatches(e -> e instanceof ResponseStatusException ex
                        && ex.getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    // --- getSeasonPerformance ---

    @Test
    void getSeasonPerformance_withExistingStats_returnsPerformanceWithNullPhase() {
        String playerId = "player-1";
        String seasonId = PAST_SEASON.id();
        when(seasonService.getSeasonById(seasonId)).thenReturn(Mono.just(PAST_SEASON));
        when(playerSeasonStatsRepository.findByPlayerIdAndSeasonId(playerId, seasonId))
                .thenReturn(Mono.just(statsEntity(playerId, seasonId)));

        StepVerifier.create(performanceService.getSeasonPerformance(playerId, seasonId))
                .assertNext(response -> {
                    assertThat(response.season().id()).isEqualTo(seasonId);
                    assertThat(response.currentPhase()).isNull();
                    assertThat(response.rank()).isEqualTo(PlayerRank.INTERMEDIATE.name());
                    assertThat(response.totalMatches()).isEqualTo(10);
                })
                .verifyComplete();
    }

    @Test
    void getSeasonPerformance_whenPlayerDidNotParticipate_returns404() {
        String playerId = "player-1";
        String seasonId = PAST_SEASON.id();
        when(seasonService.getSeasonById(seasonId)).thenReturn(Mono.just(PAST_SEASON));
        when(playerSeasonStatsRepository.findByPlayerIdAndSeasonId(playerId, seasonId))
                .thenReturn(Mono.empty());

        StepVerifier.create(performanceService.getSeasonPerformance(playerId, seasonId))
                .expectErrorMatches(e -> e instanceof ResponseStatusException ex
                        && ex.getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void getSeasonPerformance_whenSeasonNotFound_propagatesError() {
        String playerId = "player-1";
        String seasonId = "unknown-season";
        when(seasonService.getSeasonById(seasonId))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Season not found")));

        StepVerifier.create(performanceService.getSeasonPerformance(playerId, seasonId))
                .expectErrorMatches(e -> e instanceof ResponseStatusException ex
                        && ex.getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    // --- getParticipatedSeasons ---

    @Test
    void getParticipatedSeasons_returnsSeasonsSortedByYearAndNumberDescending() {
        String playerId = "player-1";
        Season season2025_1 = new Season("s-2025-1", 2025, 1, null,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 28),
                LocalDate.of(2025, 2, 1), SeasonStatus.FINISHED, null);
        Season season2025_2 = new Season("s-2025-2", 2025, 2, null,
                LocalDate.of(2025, 7, 1), LocalDate.of(2025, 8, 31),
                LocalDate.of(2025, 8, 1), SeasonStatus.FINISHED, null);

        when(seasonService.getSeasonsByPlayer(playerId))
                .thenReturn(Flux.just(season2025_1, season2025_2));

        StepVerifier.create(performanceService.getParticipatedSeasons(playerId))
                .assertNext(r -> assertThat(r.id()).isEqualTo("s-2025-2"))
                .assertNext(r -> assertThat(r.id()).isEqualTo("s-2025-1"))
                .verifyComplete();
    }

    @Test
    void getParticipatedSeasons_whenNoSeasons_returnsEmpty() {
        String playerId = "player-new";
        when(seasonService.getSeasonsByPlayer(playerId)).thenReturn(Flux.empty());

        StepVerifier.create(performanceService.getParticipatedSeasons(playerId))
                .verifyComplete();
    }
}
