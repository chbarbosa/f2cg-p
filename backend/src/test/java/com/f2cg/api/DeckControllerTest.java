package com.f2cg.api;

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
class DeckControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private DatabaseClient databaseClient;

    private static final String REGISTER_URL = "/api/auth/register";
    private static final String LOGIN_URL    = "/api/auth/login";
    private static final String DECKS_URL    = "/api/decks";
    private static final String CARDS_URL    = "/api/cards";

    private String token;

    @BeforeEach
    void setup() {
        databaseClient.sql("DELETE FROM decks").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM players").fetch().rowsUpdated().block();

        webTestClient.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"deckuser","password":"pass123"}
                        """)
                .exchange()
                .expectStatus().isCreated();

        var loginResult = webTestClient.post().uri(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"deckuser","password":"pass123"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .jsonPath("$.playerId").isNotEmpty()
                .returnResult();

        String responseBody = new String(loginResult.getResponseBody());
        token = extractJsonField(responseBody, "token");
    }

    @AfterEach
    void cleanup() {
        databaseClient.sql("DELETE FROM decks").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM players").fetch().rowsUpdated().block();
    }

    @Test
    void getCards_byTheme_returns31Cards() {
        webTestClient.get().uri(CARDS_URL + "?theme=WARRIOR")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(31);
    }

    @Test
    void createDeck_noCards_returnsDraftStatus() {
        webTestClient.post().uri(DECKS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("""
                        {"name":"My Deck","theme":"WARRIOR","cardIds":[]}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo("DRAFT")
                .jsonPath("$.theme").isEqualTo("WARRIOR")
                .jsonPath("$.id").isNotEmpty();
    }

    @Test
    void createDeck_twentyCards_returnsPlayableStatus() {
        String cardIdsJson = buildCardIdsJson(20);

        webTestClient.post().uri(DECKS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"name\":\"Full Deck\",\"theme\":\"WARRIOR\",\"cardIds\":" + cardIdsJson + "}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo("PLAYABLE");
    }

    @Test
    void getDeck_returnsFullCardDetails() {
        String deckId = createDraftDeck("Detail Deck");

        webTestClient.get().uri(DECKS_URL + "/" + deckId)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.deck.id").isEqualTo(deckId)
                .jsonPath("$.cards").isArray();
    }

    @Test
    void updateDeck_addCards_changesStatusToPlayable() {
        String deckId = createDraftDeck("Update Deck");
        String cardIdsJson = buildCardIdsJson(20);

        webTestClient.put().uri(DECKS_URL + "/" + deckId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"name\":\"Update Deck\",\"theme\":\"WARRIOR\",\"cardIds\":" + cardIdsJson + "}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("PLAYABLE");
    }

    @Test
    void deleteDeck_returns204() {
        String deckId = createDraftDeck("Delete Me");

        webTestClient.delete().uri(DECKS_URL + "/" + deckId)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void createDeck_eighthDeck_returns409() {
        for (int i = 1; i <= 7; i++) {
            createDraftDeck("Deck " + i);
        }

        webTestClient.post().uri(DECKS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("""
                        {"name":"Deck 8","theme":"WARRIOR","cardIds":[]}
                        """)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void createDeck_duplicateCardIds_returns400() {
        webTestClient.post().uri(DECKS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("""
                        {"name":"Bad Deck","theme":"WARRIOR","cardIds":["w-u-01","w-u-01"]}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createDeck_wrongThemeCard_returns400() {
        webTestClient.post().uri(DECKS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("""
                        {"name":"Bad Deck","theme":"WARRIOR","cardIds":["m-u-01"]}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createDeck_withoutToken_returns401() {
        webTestClient.post().uri(DECKS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"No Auth","theme":"WARRIOR","cardIds":[]}
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void listDecks_returnsPlayerDecks() {
        createDraftDeck("Deck A");
        createDraftDeck("Deck B");

        webTestClient.get().uri(DECKS_URL)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }

    // --- helpers ---

    private String createDraftDeck(String name) {
        var result = webTestClient.post().uri(DECKS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"name\":\"" + name + "\",\"theme\":\"WARRIOR\",\"cardIds\":[]}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult();

        String body = new String(result.getResponseBody());
        return extractJsonField(body, "id");
    }

    // Uses the 20 seeded WARRIOR unit cards exactly
    private static final java.util.List<String> WARRIOR_20_IDS = java.util.List.of(
            "w-u-01","w-u-02","w-u-03","w-u-04","w-u-05",
            "w-u-06","w-u-07","w-u-08","w-u-09","w-u-10",
            "w-u-11","w-u-12","w-u-13","w-u-14","w-u-15",
            "w-u-16","w-u-17","w-u-18","w-u-19","w-u-20"
    );

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
