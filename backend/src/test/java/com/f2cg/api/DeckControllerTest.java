package com.f2cg.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeckControllerTest extends BaseControllerTest {

    private static final String DECKS_URL = "/api/decks";

    private static final String DECK_USER = "deckuser@test.com";

    private String token;

    @BeforeEach
    void setup() {
        databaseClient.sql("DELETE FROM decks").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM players").fetch().rowsUpdated().block();

        token = registerAndLogin(DECK_USER);
    }

    @AfterEach
    void cleanup() {
        databaseClient.sql("DELETE FROM decks").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM players").fetch().rowsUpdated().block();
    }

    @Test
    void createDeck_noCards_returnsDraftStatus() {
        webTestClient.post().uri(DECKS_URL)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
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
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
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
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
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
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
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
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
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
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
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
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
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
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"name\":\"" + name + "\",\"theme\":\"WARRIOR\",\"cardIds\":[]}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult();

        String body = new String(result.getResponseBody());
        return extractJsonField(body, "id");
    }
}