package com.f2cg.api;

import com.f2cg.infrastructure.r2dbc.PlayerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class QueueControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private PlayerRepository playerRepository;

    private static final String REGISTER_URL = "/api/auth/register";
    private static final String VERIFY_URL   = "/api/auth/verify";
    private static final String LOGIN_URL    = "/api/auth/login";
    private static final String DECKS_URL    = "/api/decks";
    private static final String QUEUE_URL    = "/api/queue";

    private static final String QUEUE_USER        = "queueuser@test.com";
    private static final String QUEUE_USER_2      = "queueuser2@test.com";

    private static final java.util.List<String> WARRIOR_20_IDS = java.util.List.of(
            "w-u-01","w-u-02","w-u-03","w-u-04","w-u-05",
            "w-u-06","w-u-07","w-u-08","w-u-09","w-u-10",
            "w-u-11","w-u-12","w-u-13","w-u-14","w-u-15",
            "w-u-16","w-u-17","w-u-18","w-u-19","w-u-20"
    );

    private String token;
    private String playableDeckId;

    @BeforeEach
    void setup() {
        databaseClient.sql("DELETE FROM queue_entries").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM player_season_stats").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM seasons").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM decks").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM players").fetch().rowsUpdated().block();

        token = registerAndLogin(QUEUE_USER);
        playableDeckId = createPlayableDeck(token);
    }

    @AfterEach
    void cleanup() {
        databaseClient.sql("DELETE FROM queue_entries").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM player_season_stats").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM seasons").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM decks").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM players").fetch().rowsUpdated().block();
    }

    @Test
    void fullFlow_joinGetStatusCancel() {
        insertFreePhaseSeason();

        // POST → join queue
        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"deckId\":\"" + playableDeckId + "\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo("WAITING")
                .jsonPath("$.deckId").isEqualTo(playableDeckId)
                .jsonPath("$.id").isNotEmpty();

        // GET /status → returns WAITING entry
        webTestClient.get().uri(QUEUE_URL + "/status")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("WAITING");

        // DELETE → cancel
        webTestClient.delete().uri(QUEUE_URL)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();

        // GET /status → 404
        webTestClient.get().uri(QUEUE_URL + "/status")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void joinQueue_withoutToken_returns401() {
        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"deckId\":\"" + playableDeckId + "\"}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void joinQueue_deckNotFound_returns404() {
        insertFreePhaseSeason();

        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"deckId\":\"nonexistent-deck-id\"}")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void joinQueue_draftDeck_returns400() {
        insertFreePhaseSeason();

        var draftResult = webTestClient.post().uri(DECKS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"name\":\"DraftDeck\",\"theme\":\"WARRIOR\",\"cardIds\":[]}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult();

        String draftDeckId = extractJsonField(new String(draftResult.getResponseBody()), "id");

        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"deckId\":\"" + draftDeckId + "\"}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void joinQueue_twice_returns409() {
        insertFreePhaseSeason();

        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"deckId\":\"" + playableDeckId + "\"}")
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"deckId\":\"" + playableDeckId + "\"}")
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void cancelQueue_noEntry_returns404() {
        webTestClient.delete().uri(QUEUE_URL)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getStatus_noEntry_returns404() {
        webTestClient.get().uri(QUEUE_URL + "/status")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    // --- Season & Rank E2E tests ---

    @Test
    void joinQueue_freePhase_matchmakingRankIsNull() {
        insertFreePhaseSeason();

        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"deckId\":\"" + playableDeckId + "\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.matchmakingRank").doesNotExist();
    }

    @Test
    void joinQueue_rankedPhase_elitePlayer_matchmakingRankIsElite() {
        String seasonId = insertRankedPhaseSeason();
        String playerId = playerRepository.findByUsername(QUEUE_USER)
                .map(p -> p.getId())
                .block();
        insertPlayerStats(playerId, seasonId, "ELITE");

        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"deckId\":\"" + playableDeckId + "\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.matchmakingRank").isEqualTo("ELITE");
    }

    @Test
    void joinQueue_rankedPhase_pendingPlayer_matchmakingRankIsRookie() {
        insertRankedPhaseSeason();
        // no player_season_stats → player is PENDING → treated as ROOKIE

        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"deckId\":\"" + playableDeckId + "\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.matchmakingRank").isEqualTo("ROOKIE");
    }

    @Test
    void joinQueue_rankedPhase_pendingAndRookiePlayers_haveSameMatchmakingRank() {
        String seasonId = insertRankedPhaseSeason();

        // Player 2 (ROOKIE)
        String token2 = registerAndLogin(QUEUE_USER_2);
        String deck2 = createPlayableDeck(token2);
        String player2Id = playerRepository.findByUsername(QUEUE_USER_2)
                .map(p -> p.getId())
                .block();
        insertPlayerStats(player2Id, seasonId, "ROOKIE");

        // Player 1 joins (no stats → PENDING → ROOKIE)
        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"deckId\":\"" + playableDeckId + "\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.matchmakingRank").isEqualTo("ROOKIE");

        // Player 2 joins (ROOKIE)
        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token2)
                .bodyValue("{\"deckId\":\"" + deck2 + "\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.matchmakingRank").isEqualTo("ROOKIE");
    }

    // --- helpers ---

    private String registerAndLogin(String email) {
        webTestClient.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"" + email + "\",\"password\":\"pass123\"}")
                .exchange()
                .expectStatus().isCreated();

        String code = playerRepository.findByUsername(email)
                .map(p -> p.getActivationCode())
                .block();

        webTestClient.post().uri(VERIFY_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}")
                .exchange()
                .expectStatus().isOk();

        var loginResult = webTestClient.post().uri(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"" + email + "\",\"password\":\"pass123\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .returnResult();

        return extractJsonField(new String(loginResult.getResponseBody()), "token");
    }

    private String createPlayableDeck(String authToken) {
        String cardIdsJson = buildCardIdsJson(20);
        var deckResult = webTestClient.post().uri(DECKS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + authToken)
                .bodyValue("{\"name\":\"PlayableDeck\",\"theme\":\"WARRIOR\",\"cardIds\":" + cardIdsJson + "}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult();

        return extractJsonField(new String(deckResult.getResponseBody()), "id");
    }

    private void insertFreePhaseSeason() {
        LocalDate today = LocalDate.now();
        String id = UUID.randomUUID().toString();
        databaseClient.sql("""
                INSERT INTO seasons (id, season_year, season_number, name, start_date, end_date,
                    phase2_start_date, status)
                VALUES (:id, :year, 1, 'Test Season', :start, :end, :phase2, 'ACTIVE')
                """)
                .bind("id", id)
                .bind("year", today.getYear())
                .bind("start", today.minusDays(10))
                .bind("end", today.plusDays(50))
                .bind("phase2", today.plusDays(20)) // phase2 in the future → FREE now
                .fetch().rowsUpdated().block();
    }

    private String insertRankedPhaseSeason() {
        LocalDate today = LocalDate.now();
        String id = UUID.randomUUID().toString();
        databaseClient.sql("""
                INSERT INTO seasons (id, season_year, season_number, name, start_date, end_date,
                    phase2_start_date, status)
                VALUES (:id, :year, 1, 'Test Season', :start, :end, :phase2, 'ACTIVE')
                """)
                .bind("id", id)
                .bind("year", today.getYear())
                .bind("start", today.minusDays(30))
                .bind("end", today.plusDays(30))
                .bind("phase2", today.minusDays(10)) // phase2 in the past → RANKED now
                .fetch().rowsUpdated().block();
        return id;
    }

    private void insertPlayerStats(String playerId, String seasonId, String rank) {
        databaseClient.sql("""
                INSERT INTO player_season_stats
                    (id, player_id, season_id, total_matches, victories, defeats,
                     rank, highest_rank, matches_this_week)
                VALUES (:id, :playerId, :seasonId, 10, 5, 5, :rank, :rank, 3)
                """)
                .bind("id", UUID.randomUUID().toString())
                .bind("playerId", playerId)
                .bind("seasonId", seasonId)
                .bind("rank", rank)
                .fetch().rowsUpdated().block();
    }

    private String buildCardIdsJson(int count) {
        String ids = WARRIOR_20_IDS.stream()
                .limit(count)
                .map(id -> "\"" + id + "\"")
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        return "[" + ids + "]";
    }

    private String extractJsonField(String json, String field) {
        String search = "\"" + field + "\":\"";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}