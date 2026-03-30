package com.f2cg.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CardControllerTest extends BaseControllerTest {

    private static final String CARDS_URL = "/api/cards";
    private static final String CARD_USER = "carduser@test.com";

    private String token;

    @BeforeEach
    void setup() {
        databaseClient.sql("DELETE FROM player_season_stats").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM players").fetch().rowsUpdated().block();
        token = registerAndLogin(CARD_USER);
    }

    @AfterEach
    void cleanup() {
        databaseClient.sql("DELETE FROM player_season_stats").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM players").fetch().rowsUpdated().block();
    }

    @Test
    void getCards_warrior_returns31Cards() {
        webTestClient.get().uri(CARDS_URL + "?theme=WARRIOR")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(31);
    }

    @Test
    void getCards_responseContainsExpectedFields() {
        webTestClient.get().uri(CARDS_URL + "?theme=WARRIOR")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isNotEmpty()
                .jsonPath("$[0].name").isNotEmpty()
                .jsonPath("$[0].manaCost").isNumber()
                .jsonPath("$[0].cardType").isNotEmpty()
                .jsonPath("$[0].theme").isEqualTo("WARRIOR");
    }

    @Test
    void getCards_noAuth_returns401() {
        webTestClient.get().uri(CARDS_URL + "?theme=WARRIOR")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getCards_missingTheme_returns400() {
        webTestClient.get().uri(CARDS_URL)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isBadRequest();
    }
}