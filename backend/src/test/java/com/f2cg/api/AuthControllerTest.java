package com.f2cg.api;

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
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private DatabaseClient databaseClient;

    private static final String REGISTER_URL = "/api/auth/register";
    private static final String LOGIN_URL = "/api/auth/login";

    @BeforeEach
    void cleanDb() {
        databaseClient.sql("DELETE FROM players").fetch().rowsUpdated().block();
    }

    @Test
    void register_returns201WithToken() {
        webTestClient.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"alice","password":"secret"}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.playerId").isNotEmpty()
                .jsonPath("$.token").isNotEmpty();
    }

    @Test
    void register_duplicateUsername_returns409() {
        String body = """
                {"username":"bob","password":"secret"}
                """;

        webTestClient.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void login_returns200WithToken() {
        String body = """
                {"username":"carol","password":"mypassword"}
                """;

        webTestClient.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.playerId").isNotEmpty()
                .jsonPath("$.token").isNotEmpty();
    }

    @Test
    void login_wrongPassword_returns401() {
        webTestClient.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"dave","password":"correct"}
                        """)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"dave","password":"wrong"}
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void login_unknownUser_returns401() {
        webTestClient.post().uri(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"nobody","password":"pass"}
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}