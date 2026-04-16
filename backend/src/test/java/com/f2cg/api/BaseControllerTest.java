package com.f2cg.api;

import com.f2cg.infrastructure.r2dbc.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
abstract class BaseControllerTest {

    @Autowired protected WebTestClient webTestClient;
    @Autowired protected DatabaseClient databaseClient;
    @Autowired protected PlayerRepository playerRepository;

    protected static final String REGISTER_URL = "/api/auth/register";
    protected static final String VERIFY_URL   = "/api/auth/verify";
    protected static final String LOGIN_URL    = "/api/auth/login";

    protected static final List<String> WARRIOR_20_IDS = List.of(
            "w-u-01","w-u-02","w-u-03","w-u-04","w-u-05",
            "w-u-06","w-u-07","w-u-08","w-u-09","w-u-10",
            "w-u-11","w-u-12","w-u-13","w-u-14","w-u-15",
            "w-u-16","w-u-17","w-u-18","w-u-19","w-u-20"
    );

    protected String registerAndLogin(String email) {
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

    protected String buildCardIdsJson(int count) {
        String ids = WARRIOR_20_IDS.stream()
                .limit(count)
                .map(id -> "\"" + id + "\"")
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        return "[" + ids + "]";
    }

    protected void setNickname(String authToken, String nickname) {
        webTestClient.put().uri("/api/player/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + authToken)
                .bodyValue("{\"nickname\":\"" + nickname + "\"}")
                .exchange()
                .expectStatus().isNoContent();
    }

    protected String extractJsonField(String json, String field) {
        String search = "\"" + field + "\":\"";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}