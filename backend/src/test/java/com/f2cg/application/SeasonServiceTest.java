package com.f2cg.application;

import com.f2cg.domain.season.Season;
import com.f2cg.domain.season.SeasonPhase;
import com.f2cg.domain.season.SeasonStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SeasonServiceTest {

    @Mock
    private com.f2cg.infrastructure.r2dbc.SeasonRepository seasonRepository;
    @Mock
    private com.f2cg.infrastructure.r2dbc.PlayerSeasonStatsRepository playerSeasonStatsRepository;

    private SeasonService seasonService;

    // Season 1 2026: Jan–Feb, phase2 starts Feb 1
    private static final Season SEASON = new Season(
            "s-2026-1", 2026, 1, null,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 2, 28),
            LocalDate.of(2026, 2, 1),
            SeasonStatus.ACTIVE,
            null
    );

    @BeforeEach
    void setUp() {
        seasonService = new SeasonService(seasonRepository, playerSeasonStatsRepository);
    }

    @Test
    void getCurrentPhase_duringFirstMonth_returnsFree() {
        LocalDate today = LocalDate.of(2026, 1, 15);
        assertThat(seasonService.getCurrentPhase(SEASON, today)).isEqualTo(SeasonPhase.FREE);
    }

    @Test
    void getCurrentPhase_duringSecondMonth_returnsRanked() {
        LocalDate today = LocalDate.of(2026, 2, 15);
        assertThat(seasonService.getCurrentPhase(SEASON, today)).isEqualTo(SeasonPhase.RANKED);
    }

    @Test
    void getCurrentPhase_onBoundaryDate_returnsRanked() {
        LocalDate today = LocalDate.of(2026, 2, 1);
        assertThat(seasonService.getCurrentPhase(SEASON, today)).isEqualTo(SeasonPhase.RANKED);
    }
}