package com.f2cg.api;

import com.f2cg.application.QueueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.UUID;

class MatchmakingControllerTest extends BaseControllerTest {

    private static final String QUEUE_URL = "/api/queue";
    private static final String GAME_URL  = "/api/game";

    private static final String USER1 = "match1@test.com";
    private static final String USER2 = "match2@test.com";
    private static final String USER3 = "match3@test.com";
    private static final String USER4 = "match4@test.com";
    @Autowired
    private QueueService queueService;

    private String token1;
    private String token2;
    private String deck1;
    private String deck2;

    @BeforeEach
    void setup() {
        databaseClient.sql("DELETE FROM queue_entries").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM games").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM player_season_stats").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM seasons").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM decks").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM players").fetch().rowsUpdated().block();

        insertFreePhaseSeason();
        token1 = registerAndLogin(USER1);
        token2 = registerAndLogin(USER2);
        setNickname(token1, "Player1");
        setNickname(token2, "Player2");
        deck1 = createPlayableDeck(token1);
        deck2 = createPlayableDeck(token2);
    }

    @AfterEach
    void cleanup() {
        databaseClient.sql("DELETE FROM queue_entries").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM games").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM player_season_stats").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM seasons").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM decks").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM players").fetch().rowsUpdated().block();
    }

    @Test
    void twoPlayersJoin_bothGetMatchedStatus() {
        // Player 1 joins — no opponent yet, stays WAITING
        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token1)
                .bodyValue("{\"deckId\":\"" + deck1 + "\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo("WAITING");

        // Player 2 joins — finds player 1, both matched
        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token2)
                .bodyValue("{\"deckId\":\"" + deck2 + "\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo("MATCHED");

        // Player 1 status is also MATCHED
        webTestClient.get().uri(QUEUE_URL + "/status")
                .header("Authorization", "Bearer " + token1)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("MATCHED");
    }

    @Test
    void getGame_returnsCorrectUsernames() {
        // Both players join and match
        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token1)
                .bodyValue("{\"deckId\":\"" + deck1 + "\"}")
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token2)
                .bodyValue("{\"deckId\":\"" + deck2 + "\"}")
                .exchange()
                .expectStatus().isCreated();

        String gamePublicId = databaseClient
                .sql("SELECT public_id FROM games LIMIT 1")
                .map(row -> row.get("public_id", String.class))
                .one()
                .block();

        webTestClient.get().uri(GAME_URL + "/" + gamePublicId)
                .header("Authorization", "Bearer " + token1)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.publicId").isEqualTo(gamePublicId)
                .jsonPath("$.player1Username").isNotEmpty()
                .jsonPath("$.player2Username").isNotEmpty()
                .jsonPath("$.status").isEqualTo("WAITING_START");
    }

    @Test
    void getGame_byNonParticipant_returns403() {
        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token1)
                .bodyValue("{\"deckId\":\"" + deck1 + "\"}")
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token2)
                .bodyValue("{\"deckId\":\"" + deck2 + "\"}")
                .exchange()
                .expectStatus().isCreated();

        String gamePublicId = databaseClient
                .sql("SELECT public_id FROM games LIMIT 1")
                .map(row -> row.get("public_id", String.class))
                .one()
                .block();

        String token3 = registerAndLogin(USER3);
        webTestClient.get().uri(GAME_URL + "/" + gamePublicId)
                .header("Authorization", "Bearer " + token3)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void getGame_notFound_returns404() {
        webTestClient.get().uri(GAME_URL + "/nonexistent-public-id")
                .header("Authorization", "Bearer " + token1)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void twoSeparatePairs_matchIndependently() {
        String token3 = registerAndLogin(USER3);
        String token4 = registerAndLogin(USER4);
        setNickname(token3, "Player3");
        setNickname(token4, "Player4");
        String deck3 = createPlayableDeck(token3);
        String deck4 = createPlayableDeck(token4);

        // All four join queue
        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token1)
                .bodyValue("{\"deckId\":\"" + deck1 + "\"}")
                .exchange().expectStatus().isCreated();

        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token2)
                .bodyValue("{\"deckId\":\"" + deck2 + "\"}")
                .exchange().expectStatus().isCreated();

        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token3)
                .bodyValue("{\"deckId\":\"" + deck3 + "\"}")
                .exchange().expectStatus().isCreated();

        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token4)
                .bodyValue("{\"deckId\":\"" + deck4 + "\"}")
                .exchange().expectStatus().isCreated();

        // Two separate games created
        Long gameCount = databaseClient
                .sql("SELECT COUNT(*) AS cnt FROM games")
                .map(row -> row.get("cnt", Long.class))
                .one()
                .block();
        assert gameCount != null;
        assert gameCount == 2;

        // All four players have MATCHED status
        for (String token : new String[]{token1, token2, token3, token4}) {
            webTestClient.get().uri(QUEUE_URL + "/status")
                    .header("Authorization", "Bearer " + token)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("MATCHED");
        }
    }

    @Test
    void singlePlayer_noOpponent_staysWaiting() {
        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token1)
                .bodyValue("{\"deckId\":\"" + deck1 + "\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo("WAITING");

        Long gameCount = databaseClient
                .sql("SELECT COUNT(*) AS cnt FROM games")
                .map(row -> row.get("cnt", Long.class))
                .one()
                .block();
        assert gameCount != null;
        assert gameCount == 0;
    }

    @Test
    void timedOutEntry_checkTimeouts_setsStatusTimedOut() {
        // Insert a stale WAITING entry (60 seconds old — exceeds 30s timeout)
        String stalePlayerId = playerRepository.findByUsername(USER1)
                .map(p -> p.getId())
                .block();

        databaseClient.sql("""
                INSERT INTO queue_entries (id, player_id, deck_id, status, matchmaking_rank, joined_at)
                VALUES (:id, :playerId, :deckId, 'WAITING', NULL, :joinedAt)
                """)
                .bind("id", UUID.randomUUID().toString())
                .bind("playerId", stalePlayerId)
                .bind("deckId", deck1)
                .bind("joinedAt", java.time.LocalDateTime.now().minusSeconds(60))
                .fetch().rowsUpdated().block();

        queueService.checkTimeouts();

        // Allow the reactive subscribe chain to complete
        Long timedOutCount = databaseClient
                .sql("SELECT COUNT(*) AS cnt FROM queue_entries WHERE status = 'TIMED_OUT'")
                .map(row -> row.get("cnt", Long.class))
                .one()
                .block();
        assert timedOutCount != null;
        assert timedOutCount == 1;
    }

    // --- helpers ---

    private String createPlayableDeck(String authToken) {
        String cardIdsJson = buildCardIdsJson(20);
        var result = webTestClient.post().uri("/api/decks")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + authToken)
                .bodyValue("{\"name\":\"Deck\",\"theme\":\"WARRIOR\",\"cardIds\":" + cardIdsJson + "}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult();
        return extractJsonField(new String(result.getResponseBody()), "id");
    }

    private void insertFreePhaseSeason() {
        LocalDate today = LocalDate.now();
        databaseClient.sql("""
                INSERT INTO seasons (id, season_year, season_number, name, start_date, end_date,
                    phase2_start_date, status)
                VALUES (:id, :year, 1, 'Test Season', :start, :end, :phase2, 'ACTIVE')
                """)
                .bind("id", UUID.randomUUID().toString())
                .bind("year", today.getYear())
                .bind("start", today.minusDays(10))
                .bind("end", today.plusDays(50))
                .bind("phase2", today.plusDays(20))
                .fetch().rowsUpdated().block();
    }
}