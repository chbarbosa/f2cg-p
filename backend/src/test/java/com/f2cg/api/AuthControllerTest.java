package com.f2cg.api;

import com.f2cg.infrastructure.r2dbc.PlayerRepository;
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

    @Autowired
    private PlayerRepository playerRepository;

    private static final String REGISTER_URL = "/api/auth/register";
    private static final String VERIFY_URL = "/api/auth/verify";
    private static final String LOGIN_URL = "/api/auth/login";

    @BeforeEach
    void cleanDb() {
        databaseClient.sql("DELETE FROM players").fetch().rowsUpdated().block();
    }

    @Test
    void register_returns201WithMessage() {
        webTestClient.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"alice@example.com","password":"secret"}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.message").isEqualTo("VERIFICATION_SENT");
    }

    @Test
    void register_invalidEmail_returns400() {
        webTestClient.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"not-an-email","password":"secret"}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void register_duplicateEmail_returns409() {
        String body = """
                {"username":"bob@example.com","password":"secret"}
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
    void verify_success_returnsToken() {
        // Register
        webTestClient.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"carol@example.com","password":"mypassword"}
                        """)
                .exchange()
                .expectStatus().isCreated();

        // Fetch the code directly from the DB
        String code = playerRepository.findByUsername("carol@example.com")
                .map(p -> p.getActivationCode())
                .block();

        webTestClient.post().uri(VERIFY_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"carol@example.com\",\"code\":\"" + code + "\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.playerId").isNotEmpty()
                .jsonPath("$.token").isNotEmpty();
    }

    @Test
    void verify_wrongCode_returns400() {
        webTestClient.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"dave@example.com","password":"secret"}
                        """)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri(VERIFY_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"email":"dave@example.com","code":"00000"}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void login_beforeActivation_returns403() {
        webTestClient.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"eve@example.com","password":"mypassword"}
                        """)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"eve@example.com","password":"mypassword"}
                        """)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void login_afterActivation_returns200WithToken() {
        webTestClient.post().uri(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"frank@example.com","password":"mypassword"}
                        """)
                .exchange()
                .expectStatus().isCreated();

        String code = playerRepository.findByUsername("frank@example.com")
                .map(p -> p.getActivationCode())
                .block();

        webTestClient.post().uri(VERIFY_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"frank@example.com\",\"code\":\"" + code + "\"}")
                .exchange()
                .expectStatus().isOk();

        webTestClient.post().uri(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"frank@example.com","password":"mypassword"}
                        """)
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
                        {"username":"grace@example.com","password":"correct"}
                        """)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"grace@example.com","password":"wrong"}
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void login_unknownUser_returns401() {
        webTestClient.post().uri(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"nobody@example.com","password":"pass"}
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}