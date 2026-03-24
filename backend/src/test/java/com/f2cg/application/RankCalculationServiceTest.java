package com.f2cg.application;

import com.f2cg.domain.season.PlayerRank;
import com.f2cg.domain.season.Season;
import com.f2cg.domain.season.SeasonPhase;
import com.f2cg.domain.season.SeasonStatus;
import com.f2cg.infrastructure.r2dbc.PlayerSeasonStatsEntity;
import com.f2cg.infrastructure.r2dbc.PlayerSeasonStatsRepository;
import com.f2cg.infrastructure.r2dbc.SeasonEntity;
import com.f2cg.infrastructure.r2dbc.SeasonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankCalculationServiceTest {

    @Mock
    private PlayerSeasonStatsRepository playerSeasonStatsRepository;
    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private SeasonService seasonService;

    private RankCalculationService rankCalculationService;

    // Season in FREE phase (today is in first month)
    private static final Season FREE_SEASON = new Season(
            "s-1", 2026, 1, null,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 2, 28),
            LocalDate.of(2026, 2, 1),
            SeasonStatus.ACTIVE,
            null
    );

    // Season in RANKED phase (today is in second month)
    private static final Season RANKED_SEASON = new Season(
            "s-2", 2026, 2, null,
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 4, 30),
            LocalDate.of(2026, 4, 1),
            SeasonStatus.ACTIVE,
            null
    );

    @BeforeEach
    void setUp() {
        rankCalculationService = new RankCalculationService(
                playerSeasonStatsRepository, seasonRepository, seasonService);
    }

    // --- isWeeklyCalculationDue ---

    @Test
    void isWeeklyCalculationDue_nullLastCalculation_returnsTrue() {
        Season season = seasonWith(null);
        assertThat(rankCalculationService.isWeeklyCalculationDue(season)).isTrue();
    }

    @Test
    void isWeeklyCalculationDue_lastCalculationBeforeThisMonday_returnsTrue() {
        LocalDate lastMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate previousWeek = lastMonday.minusDays(1);
        Season season = seasonWith(previousWeek);
        assertThat(rankCalculationService.isWeeklyCalculationDue(season)).isTrue();
    }

    @Test
    void isWeeklyCalculationDue_lastCalculationThisWeek_returnsFalse() {
        LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Season season = seasonWith(thisMonday);
        assertThat(rankCalculationService.isWeeklyCalculationDue(season)).isFalse();
    }

    // --- calculateRanksIfDue ---

    @Test
    void calculateRanksIfDue_notDue_doesNotRunCalculation() {
        LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Season season = seasonWith(thisMonday);

        StepVerifier.create(rankCalculationService.calculateRanksIfDue(season))
                .verifyComplete();

        verifyNoInteractions(playerSeasonStatsRepository);
    }

    @Test
    void calculateRanksIfDue_due_runsCalculation() {
        Season season = seasonWith(null);
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.FREE);
        when(playerSeasonStatsRepository.findBySeasonId(anyString())).thenReturn(Flux.empty());
        when(playerSeasonStatsRepository.saveAll(any(Iterable.class))).thenReturn(Flux.empty());
        when(seasonRepository.findById(anyString())).thenReturn(Mono.just(seasonEntity("s-1")));
        when(seasonRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rankCalculationService.calculateRanksIfDue(season))
                .verifyComplete();

        verify(playerSeasonStatsRepository).findBySeasonId(anyString());
    }

    // --- calculateRanks: distribution ---

    @Test
    void calculateRanks_freePhase_top10PercentIsElite() {
        // 10 eligible players with decreasing win rates, all past first week
        Season season = new Season("s-1", 2026, 1, null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 2, 1),
                SeasonStatus.ACTIVE,
                null);
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.FREE);

        List<PlayerSeasonStatsEntity> stats = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            stats.add(statsEntity("p-" + i, "s-1", 20, 10 - i, i));
        }
        when(playerSeasonStatsRepository.findBySeasonId("s-1")).thenReturn(Flux.fromIterable(stats));
        when(playerSeasonStatsRepository.saveAll(any(Iterable.class)))
                .thenAnswer(inv -> Flux.fromIterable(inv.getArgument(0)));
        when(seasonRepository.findById("s-1")).thenReturn(Mono.just(seasonEntity("s-1")));
        when(seasonRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rankCalculationService.calculateRanks(season))
                .verifyComplete();

        // top 10% of 10 = ceil(1) = 1 player
        assertThat(stats.get(0).getRank()).isEqualTo(PlayerRank.ELITE.name());
    }

    @Test
    void calculateRanks_freePhase_next30PercentIsAdvanced() {
        Season season = freeSeason();
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.FREE);

        List<PlayerSeasonStatsEntity> stats = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            stats.add(statsEntity("p-" + i, "s-1", 20, 10 - i, i));
        }
        when(playerSeasonStatsRepository.findBySeasonId("s-1")).thenReturn(Flux.fromIterable(stats));
        when(playerSeasonStatsRepository.saveAll(any(Iterable.class)))
                .thenAnswer(inv -> Flux.fromIterable(inv.getArgument(0)));
        when(seasonRepository.findById("s-1")).thenReturn(Mono.just(seasonEntity("s-1")));
        when(seasonRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rankCalculationService.calculateRanks(season))
                .verifyComplete();

        // elite=1, advanced=ceil(3)=3 → indices 1,2,3
        assertThat(stats.get(1).getRank()).isEqualTo(PlayerRank.ADVANCED.name());
        assertThat(stats.get(2).getRank()).isEqualTo(PlayerRank.ADVANCED.name());
        assertThat(stats.get(3).getRank()).isEqualTo(PlayerRank.ADVANCED.name());
    }

    @Test
    void calculateRanks_freePhase_next30PercentIsIntermediate() {
        Season season = freeSeason();
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.FREE);

        List<PlayerSeasonStatsEntity> stats = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            stats.add(statsEntity("p-" + i, "s-1", 20, 10 - i, i));
        }
        when(playerSeasonStatsRepository.findBySeasonId("s-1")).thenReturn(Flux.fromIterable(stats));
        when(playerSeasonStatsRepository.saveAll(any(Iterable.class)))
                .thenAnswer(inv -> Flux.fromIterable(inv.getArgument(0)));
        when(seasonRepository.findById("s-1")).thenReturn(Mono.just(seasonEntity("s-1")));
        when(seasonRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rankCalculationService.calculateRanks(season))
                .verifyComplete();

        // elite=1, adv=3, intermediate=ceil(3)=3 → indices 4,5,6
        assertThat(stats.get(4).getRank()).isEqualTo(PlayerRank.INTERMEDIATE.name());
        assertThat(stats.get(5).getRank()).isEqualTo(PlayerRank.INTERMEDIATE.name());
        assertThat(stats.get(6).getRank()).isEqualTo(PlayerRank.INTERMEDIATE.name());
    }

    @Test
    void calculateRanks_freePhase_playerBelow15Matches_isPending() {
        Season season = freeSeason();
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.FREE);

        PlayerSeasonStatsEntity lowMatches = statsEntity("p-0", "s-1", 10, 8, 2);
        when(playerSeasonStatsRepository.findBySeasonId("s-1")).thenReturn(Flux.just(lowMatches));
        when(playerSeasonStatsRepository.saveAll(any(Iterable.class)))
                .thenAnswer(inv -> Flux.fromIterable(inv.getArgument(0)));
        when(seasonRepository.findById("s-1")).thenReturn(Mono.just(seasonEntity("s-1")));
        when(seasonRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rankCalculationService.calculateRanks(season))
                .verifyComplete();

        assertThat(lowMatches.getRank()).isEqualTo(PlayerRank.PENDING.name());
    }

    @Test
    void calculateRanks_freePhase_playerInFirstWeek_isPending() {
        // Season started today — we are in the first week
        Season season = new Season("s-1", 2026, 1, null,
                LocalDate.now(),
                LocalDate.now().plusMonths(2),
                LocalDate.now().plusMonths(1),
                SeasonStatus.ACTIVE,
                null);
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.FREE);

        PlayerSeasonStatsEntity active = statsEntity("p-0", "s-1", 20, 15, 5);
        when(playerSeasonStatsRepository.findBySeasonId("s-1")).thenReturn(Flux.just(active));
        when(playerSeasonStatsRepository.saveAll(any(Iterable.class)))
                .thenAnswer(inv -> Flux.fromIterable(inv.getArgument(0)));
        when(seasonRepository.findById("s-1")).thenReturn(Mono.just(seasonEntity("s-1")));
        when(seasonRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rankCalculationService.calculateRanks(season))
                .verifyComplete();

        assertThat(active.getRank()).isEqualTo(PlayerRank.PENDING.name());
    }

    // --- calculateRanks: Phase RANKED ---

    @Test
    void calculateRanks_rankedPhase_inactivePlayer_demotedOneLevel() {
        Season season = rankedSeason();
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.RANKED);

        PlayerSeasonStatsEntity inactive = statsEntity("p-0", "s-2", 30, 20, 10);
        inactive.setMatchesThisWeek(5); // below 15 → inactive
        inactive.setRank(PlayerRank.ELITE.name());

        when(playerSeasonStatsRepository.findBySeasonId("s-2")).thenReturn(Flux.just(inactive));
        when(playerSeasonStatsRepository.saveAll(any(Iterable.class)))
                .thenAnswer(inv -> Flux.fromIterable(inv.getArgument(0)));
        when(seasonRepository.findById("s-2")).thenReturn(Mono.just(seasonEntity("s-2")));
        when(seasonRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rankCalculationService.calculateRanks(season))
                .verifyComplete();

        assertThat(inactive.getRank()).isEqualTo(PlayerRank.ADVANCED.name());
    }

    @Test
    void calculateRanks_rankedPhase_inactiveRookie_staysRookie() {
        Season season = rankedSeason();
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.RANKED);

        PlayerSeasonStatsEntity inactive = statsEntity("p-0", "s-2", 30, 10, 20);
        inactive.setMatchesThisWeek(0);
        inactive.setRank(PlayerRank.ROOKIE.name());

        when(playerSeasonStatsRepository.findBySeasonId("s-2")).thenReturn(Flux.just(inactive));
        when(playerSeasonStatsRepository.saveAll(any(Iterable.class)))
                .thenAnswer(inv -> Flux.fromIterable(inv.getArgument(0)));
        when(seasonRepository.findById("s-2")).thenReturn(Mono.just(seasonEntity("s-2")));
        when(seasonRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rankCalculationService.calculateRanks(season))
                .verifyComplete();

        assertThat(inactive.getRank()).isEqualTo(PlayerRank.ROOKIE.name());
    }

    @Test
    void calculateRanks_rankedPhase_pendingInactive_treatedAsRookie_staysRookie() {
        Season season = rankedSeason();
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.RANKED);

        PlayerSeasonStatsEntity inactive = statsEntity("p-0", "s-2", 5, 3, 2);
        inactive.setMatchesThisWeek(0);
        inactive.setRank(PlayerRank.PENDING.name());

        when(playerSeasonStatsRepository.findBySeasonId("s-2")).thenReturn(Flux.just(inactive));
        when(playerSeasonStatsRepository.saveAll(any(Iterable.class)))
                .thenAnswer(inv -> Flux.fromIterable(inv.getArgument(0)));
        when(seasonRepository.findById("s-2")).thenReturn(Mono.just(seasonEntity("s-2")));
        when(seasonRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rankCalculationService.calculateRanks(season))
                .verifyComplete();

        assertThat(inactive.getRank()).isEqualTo(PlayerRank.ROOKIE.name());
    }

    @Test
    void calculateRanks_rankedPhase_activePlayer_rankDownBlocked() {
        Season season = rankedSeason();
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.RANKED);

        // 1 active player who would be ROOKIE by distribution, but is currently ELITE → stays ELITE
        PlayerSeasonStatsEntity active = statsEntity("p-0", "s-2", 20, 10, 10);
        active.setMatchesThisWeek(20);
        active.setRank(PlayerRank.ELITE.name());
        active.setHighestRank(PlayerRank.ELITE.name());

        when(playerSeasonStatsRepository.findBySeasonId("s-2")).thenReturn(Flux.just(active));
        when(playerSeasonStatsRepository.saveAll(any(Iterable.class)))
                .thenAnswer(inv -> Flux.fromIterable(inv.getArgument(0)));
        when(seasonRepository.findById("s-2")).thenReturn(Mono.just(seasonEntity("s-2")));
        when(seasonRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rankCalculationService.calculateRanks(season))
                .verifyComplete();

        // Single active player gets top 10% = ELITE (rank-down blocked, current ELITE stays)
        assertThat(active.getRank()).isEqualTo(PlayerRank.ELITE.name());
    }

    @Test
    void calculateRanks_rankedPhase_activePlayer_rankUpAllowed() {
        Season season = rankedSeason();
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.RANKED);

        // 2 active players; top one wins more → distributed as ELITE
        PlayerSeasonStatsEntity top = statsEntity("p-0", "s-2", 20, 18, 2);
        top.setMatchesThisWeek(20);
        top.setRank(PlayerRank.ROOKIE.name());
        top.setHighestRank(PlayerRank.ROOKIE.name());

        PlayerSeasonStatsEntity second = statsEntity("p-1", "s-2", 20, 5, 15);
        second.setMatchesThisWeek(20);
        second.setRank(PlayerRank.ROOKIE.name());
        second.setHighestRank(PlayerRank.ROOKIE.name());

        when(playerSeasonStatsRepository.findBySeasonId("s-2")).thenReturn(Flux.just(top, second));
        when(playerSeasonStatsRepository.saveAll(any(Iterable.class)))
                .thenAnswer(inv -> Flux.fromIterable(inv.getArgument(0)));
        when(seasonRepository.findById("s-2")).thenReturn(Mono.just(seasonEntity("s-2")));
        when(seasonRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rankCalculationService.calculateRanks(season))
                .verifyComplete();

        assertThat(top.getRank()).isEqualTo(PlayerRank.ELITE.name());
    }

    @Test
    void calculateRanks_highestRankUpdates_whenRankImproves() {
        Season season = rankedSeason();
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.RANKED);

        PlayerSeasonStatsEntity player = statsEntity("p-0", "s-2", 20, 18, 2);
        player.setMatchesThisWeek(20);
        player.setRank(PlayerRank.ROOKIE.name());
        player.setHighestRank(PlayerRank.ROOKIE.name());

        when(playerSeasonStatsRepository.findBySeasonId("s-2")).thenReturn(Flux.just(player));
        when(playerSeasonStatsRepository.saveAll(any(Iterable.class)))
                .thenAnswer(inv -> Flux.fromIterable(inv.getArgument(0)));
        when(seasonRepository.findById("s-2")).thenReturn(Mono.just(seasonEntity("s-2")));
        when(seasonRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rankCalculationService.calculateRanks(season))
                .verifyComplete();

        // Single player → ELITE by distribution → highestRank should update
        assertThat(player.getHighestRank()).isEqualTo(PlayerRank.ELITE.name());
    }

    @Test
    void calculateRanks_highestRankDoesNotUpdate_whenRankSameOrDrops() {
        Season season = rankedSeason();
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.RANKED);

        PlayerSeasonStatsEntity player = statsEntity("p-0", "s-2", 20, 10, 10);
        player.setMatchesThisWeek(0); // inactive → demoted
        player.setRank(PlayerRank.ADVANCED.name());
        player.setHighestRank(PlayerRank.ELITE.name()); // was ELITE

        when(playerSeasonStatsRepository.findBySeasonId("s-2")).thenReturn(Flux.just(player));
        when(playerSeasonStatsRepository.saveAll(any(Iterable.class)))
                .thenAnswer(inv -> Flux.fromIterable(inv.getArgument(0)));
        when(seasonRepository.findById("s-2")).thenReturn(Mono.just(seasonEntity("s-2")));
        when(seasonRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rankCalculationService.calculateRanks(season))
                .verifyComplete();

        // Demoted from ADVANCED → INTERMEDIATE, but highestRank stays ELITE
        assertThat(player.getHighestRank()).isEqualTo(PlayerRank.ELITE.name());
    }

    @Test
    void calculateRanks_matchesThisWeekResets_afterCalculation() {
        Season season = freeSeason();
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.FREE);

        PlayerSeasonStatsEntity player = statsEntity("p-0", "s-1", 20, 15, 5);
        player.setMatchesThisWeek(7);

        when(playerSeasonStatsRepository.findBySeasonId("s-1")).thenReturn(Flux.just(player));
        when(playerSeasonStatsRepository.saveAll(any(Iterable.class)))
                .thenAnswer(inv -> Flux.fromIterable(inv.getArgument(0)));
        when(seasonRepository.findById("s-1")).thenReturn(Mono.just(seasonEntity("s-1")));
        when(seasonRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rankCalculationService.calculateRanks(season))
                .verifyComplete();

        assertThat(player.getMatchesThisWeek()).isZero();
    }

    @Test
    void calculateRanks_lastWeeklyCalculationUpdates_afterCalculation() {
        Season season = freeSeason();
        when(seasonService.getCurrentPhase(any(), any())).thenReturn(SeasonPhase.FREE);

        when(playerSeasonStatsRepository.findBySeasonId("s-1")).thenReturn(Flux.empty());
        when(playerSeasonStatsRepository.saveAll(any(Iterable.class))).thenReturn(Flux.empty());

        SeasonEntity entity = seasonEntity("s-1");
        when(seasonRepository.findById("s-1")).thenReturn(Mono.just(entity));
        when(seasonRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rankCalculationService.calculateRanks(season))
                .verifyComplete();

        verify(seasonRepository).save(argThat(e ->
                LocalDate.now().equals(e.getLastWeeklyCalculation())));
    }

    // --- getMatchmakingRank ---

    @Test
    void getMatchmakingRank_freePhase_returnsNull() {
        assertThat(rankCalculationService.getMatchmakingRank(PlayerRank.ELITE, SeasonPhase.FREE)).isNull();
        assertThat(rankCalculationService.getMatchmakingRank(PlayerRank.ROOKIE, SeasonPhase.FREE)).isNull();
        assertThat(rankCalculationService.getMatchmakingRank(PlayerRank.PENDING, SeasonPhase.FREE)).isNull();
    }

    @Test
    void getMatchmakingRank_rankedPhase_pendingReturnsRookie() {
        assertThat(rankCalculationService.getMatchmakingRank(PlayerRank.PENDING, SeasonPhase.RANKED))
                .isEqualTo(PlayerRank.ROOKIE);
    }

    @Test
    void getMatchmakingRank_rankedPhase_returnsActualRank() {
        assertThat(rankCalculationService.getMatchmakingRank(PlayerRank.ELITE, SeasonPhase.RANKED))
                .isEqualTo(PlayerRank.ELITE);
        assertThat(rankCalculationService.getMatchmakingRank(PlayerRank.ADVANCED, SeasonPhase.RANKED))
                .isEqualTo(PlayerRank.ADVANCED);
        assertThat(rankCalculationService.getMatchmakingRank(PlayerRank.INTERMEDIATE, SeasonPhase.RANKED))
                .isEqualTo(PlayerRank.INTERMEDIATE);
        assertThat(rankCalculationService.getMatchmakingRank(PlayerRank.ROOKIE, SeasonPhase.RANKED))
                .isEqualTo(PlayerRank.ROOKIE);
    }

    // --- helpers ---

    private Season seasonWith(LocalDate lastWeeklyCalculation) {
        return new Season("s-1", 2026, 1, null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 2, 1),
                SeasonStatus.ACTIVE,
                lastWeeklyCalculation);
    }

    private Season freeSeason() {
        return new Season("s-1", 2026, 1, null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 2, 1),
                SeasonStatus.ACTIVE,
                null);
    }

    private Season rankedSeason() {
        return new Season("s-2", 2026, 2, null,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 30),
                LocalDate.of(2026, 4, 1),
                SeasonStatus.ACTIVE,
                null);
    }

    private PlayerSeasonStatsEntity statsEntity(String playerId, String seasonId,
                                                 int totalMatches, int victories, int defeats) {
        PlayerSeasonStatsEntity e = new PlayerSeasonStatsEntity();
        e.setId(playerId + "-" + seasonId);
        e.setPlayerId(playerId);
        e.setSeasonId(seasonId);
        e.setTotalMatches(totalMatches);
        e.setVictories(victories);
        e.setDefeats(defeats);
        e.setRank(PlayerRank.PENDING.name());
        e.setHighestRank(PlayerRank.PENDING.name());
        e.setMatchesThisWeek(totalMatches);
        e.setLastRankUpdate(LocalDateTime.now());
        return e;
    }

    private SeasonEntity seasonEntity(String id) {
        SeasonEntity e = new SeasonEntity();
        e.setId(id);
        e.setYear(2026);
        e.setSeasonNumber(1);
        e.setStartDate(LocalDate.of(2026, 1, 1));
        e.setEndDate(LocalDate.of(2026, 2, 28));
        e.setPhase2StartDate(LocalDate.of(2026, 2, 1));
        e.setStatus("ACTIVE");
        e.setLastWeeklyCalculation(null);
        return e;
    }

    private boolean inactive(PlayerSeasonStatsEntity e) {
        return e.getMatchesThisWeek() < 15;
    }
}