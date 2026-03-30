package com.f2cg.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

class PerformanceControllerTest extends BaseControllerTest {

    private static final String CURRENT_URL  = "/api/performance/current";
    private static final String BY_SEASON_URL = "/api/performance";
    private static final String SEASONS_URL  = "/api/performance/seasons";

    private static final String PERF_USER = "perfuser@test.com";

    private String token;
    private String playerId;

    @BeforeEach
    void setup() {
        databaseClient.sql("DELETE FROM player_season_stats").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM seasons").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM players").fetch().rowsUpdated().block();

        token = registerAndLogin(PERF_USER);
        playerId = playerRepository.findByUsername(PERF_USER)
                .map(p -> p.getId())
                .block();
    }

    @AfterEach
    void cleanup() {
        databaseClient.sql("DELETE FROM player_season_stats").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM seasons").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM players").fetch().rowsUpdated().block();
    }

    // --- GET /api/performance/current ---

    @Test
    void getCurrent_noAuth_returns401() {
        webTestClient.get().uri(CURRENT_URL)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getCurrent_noActiveSeason_returns404() {
        webTestClient.get().uri(CURRENT_URL)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getCurrent_withActiveSeasonNoStats_returnsDefaultPending() {
        insertActiveSeason("season-active-1");

        webTestClient.get().uri(CURRENT_URL)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.season.id").isEqualTo("season-active-1")
                .jsonPath("$.rank").isEqualTo("PENDING")
                .jsonPath("$.highestRank").isEqualTo("PENDING")
                .jsonPath("$.totalMatches").isEqualTo(0)
                .jsonPath("$.victories").isEqualTo(0)
                .jsonPath("$.defeats").isEqualTo(0)
                .jsonPath("$.matchesThisWeek").isEqualTo(0);
    }

    @Test
    void getCurrent_withStats_returnsPlayerData() {
        String seasonId = "season-active-2";
        insertActiveSeason(seasonId);
        insertPlayerStats(playerId, seasonId);

        webTestClient.get().uri(CURRENT_URL)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.season.id").isEqualTo(seasonId)
                .jsonPath("$.rank").isEqualTo("INTERMEDIATE")
                .jsonPath("$.highestRank").isEqualTo("ADVANCED")
                .jsonPath("$.totalMatches").isEqualTo(10)
                .jsonPath("$.victories").isEqualTo(6)
                .jsonPath("$.defeats").isEqualTo(4)
                .jsonPath("$.matchesThisWeek").isEqualTo(3);
    }

    // --- GET /api/performance?seasonId=X ---

    @Test
    void getBySeason_noAuth_returns401() {
        webTestClient.get().uri(BY_SEASON_URL + "?seasonId=any")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getBySeason_seasonNotFound_returns404() {
        webTestClient.get().uri(BY_SEASON_URL + "?seasonId=unknown-season")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getBySeason_playerNotParticipated_returns404() {
        String seasonId = "season-past-1";
        insertFinishedSeason(seasonId);

        webTestClient.get().uri(BY_SEASON_URL + "?seasonId=" + seasonId)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getBySeason_playerParticipated_returnsDataWithNullPhase() {
        String seasonId = "season-past-2";
        insertFinishedSeason(seasonId);
        insertPlayerStats(playerId, seasonId);

        webTestClient.get().uri(BY_SEASON_URL + "?seasonId=" + seasonId)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.season.id").isEqualTo(seasonId)
                .jsonPath("$.currentPhase").doesNotExist()
                .jsonPath("$.rank").isEqualTo("INTERMEDIATE")
                .jsonPath("$.totalMatches").isEqualTo(10);
    }

    // --- GET /api/performance/seasons ---

    @Test
    void getSeasons_noAuth_returns401() {
        webTestClient.get().uri(SEASONS_URL)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getSeasons_noParticipation_returnsEmptyArray() {
        webTestClient.get().uri(SEASONS_URL)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    void getSeasons_withParticipation_returnsSeasonList() {
        String seasonId = "season-past-3";
        insertFinishedSeason(seasonId);
        insertPlayerStats(playerId, seasonId);

        webTestClient.get().uri(SEASONS_URL)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].id").isEqualTo(seasonId);
    }

    // --- helpers ---

    private void insertActiveSeason(String id) {
        LocalDate today = LocalDate.now();
        databaseClient.sql("""
                INSERT INTO seasons (id, season_year, season_number, name, start_date, end_date,
                    phase2_start_date, status)
                VALUES (:id, :year, 1, 'Active Season', :start, :end, :phase2, 'ACTIVE')
                """)
                .bind("id", id)
                .bind("year", today.getYear())
                .bind("start", today.minusDays(10))
                .bind("end", today.plusDays(50))
                .bind("phase2", today.plusDays(20))
                .fetch().rowsUpdated().block();
    }

    private void insertFinishedSeason(String id) {
        LocalDate today = LocalDate.now();
        databaseClient.sql("""
                INSERT INTO seasons (id, season_year, season_number, name, start_date, end_date,
                    phase2_start_date, status)
                VALUES (:id, :year, 1, 'Finished Season', :start, :end, :phase2, 'FINISHED')
                """)
                .bind("id", id)
                .bind("year", today.getYear() - 1)
                .bind("start", today.minusDays(90))
                .bind("end", today.minusDays(30))
                .bind("phase2", today.minusDays(60))
                .fetch().rowsUpdated().block();
    }

    private void insertPlayerStats(String pid, String seasonId) {
        databaseClient.sql("""
                INSERT INTO player_season_stats
                    (id, player_id, season_id, total_matches, victories, defeats,
                     rank, highest_rank, matches_this_week)
                VALUES (:id, :playerId, :seasonId, 10, 6, 4, 'INTERMEDIATE', 'ADVANCED', 3)
                """)
                .bind("id", UUID.randomUUID().toString())
                .bind("playerId", pid)
                .bind("seasonId", seasonId)
                .fetch().rowsUpdated().block();
    }
}