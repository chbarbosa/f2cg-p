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

    private static final String QUEUE_USER = "queueuser@test.com";

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
        databaseClient.sql("DELETE FROM decks").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM players").fetch().rowsUpdated().block();

        webTestClient.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"" + QUEUE_USER + "\",\"password\":\"pass123\"}")
                .exchange()
                .expectStatus().isCreated();

        String code = playerRepository.findByUsername(QUEUE_USER)
                .map(p -> p.getActivationCode())
                .block();

        webTestClient.post().uri(VERIFY_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"" + QUEUE_USER + "\",\"code\":\"" + code + "\"}")
                .exchange()
                .expectStatus().isOk();

        var loginResult = webTestClient.post().uri(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"" + QUEUE_USER + "\",\"password\":\"pass123\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .returnResult();

        String responseBody = new String(loginResult.getResponseBody());
        token = extractJsonField(responseBody, "token");

        String cardIdsJson = buildCardIdsJson(20);
        var deckResult = webTestClient.post().uri(DECKS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"name\":\"PlayableDeck\",\"theme\":\"WARRIOR\",\"cardIds\":" + cardIdsJson + "}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult();

        playableDeckId = extractJsonField(new String(deckResult.getResponseBody()), "id");
    }

    @AfterEach
    void cleanup() {
        databaseClient.sql("DELETE FROM queue_entries").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM decks").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM players").fetch().rowsUpdated().block();
    }

    @Test
    void fullFlow_joinGetStatusCancel() {
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
        webTestClient.post().uri(QUEUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"deckId\":\"nonexistent-deck-id\"}")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void joinQueue_draftDeck_returns400() {
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

    // --- helpers ---

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