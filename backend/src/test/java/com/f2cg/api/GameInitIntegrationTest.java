package com.f2cg.api;

import com.f2cg.application.CardIdConverter;
import com.f2cg.eventbus.AppEventType;
import com.f2cg.eventbus.InMemoryEventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GameInitIntegrationTest extends BaseControllerTest {

    private static final String QUEUE_URL = "/api/queue";

    private static final String USER1 = "init1@test.com";
    private static final String USER2 = "init2@test.com";

    @Autowired
    private InMemoryEventBus eventBus;

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
        eventBus.clear();

        insertFreePhaseSeason();
        token1 = registerAndLogin(USER1);
        token2 = registerAndLogin(USER2);
        setNickname(token1, "Alice");
        setNickname(token2, "Bob");
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
        eventBus.clear();
    }

    private void joinQueue(String token, String deckId) {
        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"deckId\":\"" + deckId + "\"}")
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void afterMatch_player1HandHasExactly5Cards() {
        joinQueue(token1, deck1);
        joinQueue(token2, deck2);

        String hand = databaseClient.sql("SELECT player1_hand FROM games LIMIT 1")
                .map(row -> row.get("player1_hand", String.class))
                .one().block();

        assertThat(CardIdConverter.toList(hand)).hasSize(5);
    }

    @Test
    void afterMatch_player2HandHasExactly5Cards() {
        joinQueue(token1, deck1);
        joinQueue(token2, deck2);

        String hand = databaseClient.sql("SELECT player2_hand FROM games LIMIT 1")
                .map(row -> row.get("player2_hand", String.class))
                .one().block();

        assertThat(CardIdConverter.toList(hand)).hasSize(5);
    }

    @Test
    void afterMatch_player1StackHasRemainingCards() {
        joinQueue(token1, deck1);
        joinQueue(token2, deck2);

        String stack = databaseClient.sql("SELECT player1_stack FROM games LIMIT 1")
                .map(row -> row.get("player1_stack", String.class))
                .one().block();

        // Deck has 20 cards; 5 dealt to hand, 15 remain in stack
        assertThat(CardIdConverter.toList(stack)).hasSize(15);
    }

    @Test
    void afterMatch_player2StackHasRemainingCards() {
        joinQueue(token1, deck1);
        joinQueue(token2, deck2);

        String stack = databaseClient.sql("SELECT player2_stack FROM games LIMIT 1")
                .map(row -> row.get("player2_stack", String.class))
                .one().block();

        assertThat(CardIdConverter.toList(stack)).hasSize(15);
    }

    @Test
    void afterMatch_player1HandAndStackCoverEntireDeck_noMissingNoDuplicates() {
        joinQueue(token1, deck1);
        joinQueue(token2, deck2);

        String hand = databaseClient.sql("SELECT player1_hand FROM games LIMIT 1")
                .map(row -> row.get("player1_hand", String.class))
                .one().block();
        String stack = databaseClient.sql("SELECT player1_stack FROM games LIMIT 1")
                .map(row -> row.get("player1_stack", String.class))
                .one().block();

        List<String> handIds = CardIdConverter.toList(hand);
        List<String> stackIds = CardIdConverter.toList(stack);

        List<String> combined = new java.util.ArrayList<>(handIds);
        combined.addAll(stackIds);

        assertThat(combined).hasSize(20);
        assertThat(new HashSet<>(combined)).hasSize(20);

        // Verify all IDs are from the WARRIOR_20_IDS set
        assertThat(combined).allMatch(id -> WARRIOR_20_IDS.contains(id));
    }

    @Test
    void afterMatch_player2HandAndStackCoverEntireDeck_noMissingNoDuplicates() {
        joinQueue(token1, deck1);
        joinQueue(token2, deck2);

        String hand = databaseClient.sql("SELECT player2_hand FROM games LIMIT 1")
                .map(row -> row.get("player2_hand", String.class))
                .one().block();
        String stack = databaseClient.sql("SELECT player2_stack FROM games LIMIT 1")
                .map(row -> row.get("player2_stack", String.class))
                .one().block();

        List<String> handIds = CardIdConverter.toList(hand);
        List<String> stackIds = CardIdConverter.toList(stack);

        List<String> combined = new java.util.ArrayList<>(handIds);
        combined.addAll(stackIds);

        assertThat(combined).hasSize(20);
        assertThat(new HashSet<>(combined)).hasSize(20);
        assertThat(combined).allMatch(id -> WARRIOR_20_IDS.contains(id));
    }

    @Test
    void afterMatch_gameMatchStartedEventPublished() {
        joinQueue(token1, deck1);
        joinQueue(token2, deck2);

        assertThat(eventBus.getPublishedEvents())
                .anyMatch(e -> e.eventType() == AppEventType.GAME_MATCH_STARTED);
    }

    @Test
    void afterMatch_gameMatchStartedEventContainsPlayerAndDeckIds() {
        joinQueue(token1, deck1);
        joinQueue(token2, deck2);

        var matchStarted = eventBus.getPublishedEvents().stream()
                .filter(e -> e.eventType() == AppEventType.GAME_MATCH_STARTED)
                .findFirst()
                .orElseThrow();

        assertThat(matchStarted.payload()).contains("player1Id");
        assertThat(matchStarted.payload()).contains("player2Id");
        assertThat(matchStarted.payload()).contains("player1DeckId");
        assertThat(matchStarted.payload()).contains("player2DeckId");
        assertThat(matchStarted.payload()).contains("gameId");
    }

    @Test
    void afterMatch_player1CanAccessGame_player2CanAccessGame() {
        joinQueue(token1, deck1);
        joinQueue(token2, deck2);

        String gamePublicId = databaseClient.sql("SELECT public_id FROM games LIMIT 1")
                .map(row -> row.get("public_id", String.class))
                .one().block();

        webTestClient.get().uri("/api/game/" + gamePublicId)
                .header("Authorization", "Bearer " + token1)
                .exchange()
                .expectStatus().isOk();

        webTestClient.get().uri("/api/game/" + gamePublicId)
                .header("Authorization", "Bearer " + token2)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void afterMatch_thirdPartyCannotAccessGame() {
        joinQueue(token1, deck1);
        joinQueue(token2, deck2);

        String gamePublicId = databaseClient.sql("SELECT public_id FROM games LIMIT 1")
                .map(row -> row.get("public_id", String.class))
                .one().block();

        String token3 = registerAndLogin("third@test.com");
        webTestClient.get().uri("/api/game/" + gamePublicId)
                .header("Authorization", "Bearer " + token3)
                .exchange()
                .expectStatus().isForbidden();
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