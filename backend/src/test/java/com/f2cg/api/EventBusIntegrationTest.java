package com.f2cg.api;

import com.f2cg.eventbus.AppEventType;
import com.f2cg.eventbus.InMemoryEventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventBusIntegrationTest extends BaseControllerTest {

    @Autowired
    private InMemoryEventBus eventBus;

    private static final String USER_EMAIL = "eventbus-test@example.com";
    private static final String DECKS_URL = "/api/decks";
    private static final String QUEUE_URL = "/api/queue";

    @BeforeEach
    void setup() {
        databaseClient.sql("DELETE FROM queue_entries").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM games").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM decks").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM player_season_stats").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM seasons").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM players").fetch().rowsUpdated().block();
        eventBus.clear();
    }

    @AfterEach
    void cleanup() {
        databaseClient.sql("DELETE FROM queue_entries").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM games").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM decks").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM player_season_stats").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM seasons").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM players").fetch().rowsUpdated().block();
    }

    @Test
    void login_success_publishesLoginSuccessAndLoginTimed() {
        registerAndLogin(USER_EMAIL);

        assertThat(eventBus.getPublishedEvents())
                .anyMatch(e -> e.eventType() == AppEventType.LOGIN_SUCCESS);
        assertThat(eventBus.getPublishedEvents())
                .anyMatch(e -> e.eventType() == AppEventType.LOGIN_TIMED && e.success());
    }

    @Test
    void login_failure_publishesLoginFailureAndLoginTimed() {
        // Register first so the player exists, then login with wrong password
        webTestClient.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"" + USER_EMAIL + "\",\"password\":\"pass123\"}")
                .exchange()
                .expectStatus().isCreated();

        eventBus.clear();

        webTestClient.post().uri(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"" + USER_EMAIL + "\",\"password\":\"wrong\"}")
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(eventBus.getPublishedEvents())
                .anyMatch(e -> e.eventType() == AppEventType.LOGIN_FAILURE);
        assertThat(eventBus.getPublishedEvents())
                .anyMatch(e -> e.eventType() == AppEventType.LOGIN_TIMED && !e.success());
    }

    @Test
    void joinQueue_publishesGameJoinTimed() {
        String token = registerAndLogin(USER_EMAIL);
        setNickname(token, "EventBusUser");
        String deckId = createPlayableDeck(token);
        insertFreePhaseSeason();
        eventBus.clear();

        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"deckId\":\"" + deckId + "\"}")
                .exchange()
                .expectStatus().isCreated();

        assertThat(eventBus.getPublishedEvents())
                .anyMatch(e -> e.eventType() == AppEventType.GAME_JOIN_TIMED);
    }

    @Test
    void deckCreation_publishesDeckCreated() {
        String token = registerAndLogin(USER_EMAIL);
        eventBus.clear();

        String cardIdsJson = buildCardIdsJson(20);
        webTestClient.post().uri(DECKS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"name\":\"Full Deck\",\"theme\":\"WARRIOR\",\"cardIds\":" + cardIdsJson + "}")
                .exchange()
                .expectStatus().isCreated();

        assertThat(eventBus.getPublishedEvents())
                .anyMatch(e -> e.eventType() == AppEventType.DECK_CREATED);
    }

    // --- helpers ---

    private String createPlayableDeck(String authToken) {
        String cardIdsJson = buildCardIdsJson(20);
        var result = webTestClient.post().uri(DECKS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + authToken)
                .bodyValue("{\"name\":\"PlayableDeck\",\"theme\":\"WARRIOR\",\"cardIds\":" + cardIdsJson + "}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult();
        return extractJsonField(new String(result.getResponseBody()), "id");
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
                .bind("phase2", today.plusDays(20))
                .fetch().rowsUpdated().block();
    }
}