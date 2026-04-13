package com.f2cg.eventbus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryEventBusTest {

    private InMemoryEventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = new InMemoryEventBus();
    }

    @Test
    void publish_storesEventInMemory() {
        AppEvent event = EventBuilder.create(AppEventType.LOGIN_SUCCESS)
                .actor("player-1")
                .target("player-1", "AUTH")
                .success()
                .payload(null)
                .build();

        eventBus.publish(event);

        assertThat(eventBus.getPublishedEvents()).hasSize(1);
        assertThat(eventBus.getPublishedEvents().get(0).eventType())
                .isEqualTo(AppEventType.LOGIN_SUCCESS);
        assertThat(eventBus.getPublishedEvents().get(0).actorId()).isEqualTo("player-1");
    }

    @Test
    void publish_multipleEvents_storesAllInOrder() {
        AppEvent e1 = EventBuilder.create(AppEventType.LOGIN_SUCCESS).actor("p1").target("p1", "AUTH").success().build();
        AppEvent e2 = EventBuilder.create(AppEventType.LOGIN_FAILURE).actor(null).target(null, "AUTH").failure("bad creds").build();

        eventBus.publish(e1);
        eventBus.publish(e2);

        assertThat(eventBus.getPublishedEvents()).hasSize(2);
        assertThat(eventBus.getPublishedEvents().get(0).eventType()).isEqualTo(AppEventType.LOGIN_SUCCESS);
        assertThat(eventBus.getPublishedEvents().get(1).eventType()).isEqualTo(AppEventType.LOGIN_FAILURE);
    }

    @Test
    void clear_removesAllEvents() {
        eventBus.publish(EventBuilder.create(AppEventType.LOGIN_SUCCESS).actor("p1").target("p1", "AUTH").success().build());
        eventBus.publish(EventBuilder.create(AppEventType.LOGIN_FAILURE).actor(null).target(null, "AUTH").failure("bad").build());

        eventBus.clear();

        assertThat(eventBus.getPublishedEvents()).isEmpty();
    }

    @Test
    void timed_success_publishesTimedEvent() {
        Mono<String> source = Mono.just("result");

        StepVerifier.create(eventBus.timed(AppEventType.LOGIN_TIMED, "player-1", "player-1", "AUTH", source))
                .expectNext("result")
                .verifyComplete();

        assertThat(eventBus.getPublishedEvents()).hasSize(1);
        AppEvent event = eventBus.getPublishedEvents().get(0);
        assertThat(event.eventType()).isEqualTo(AppEventType.LOGIN_TIMED);
        assertThat(event.success()).isTrue();
        assertThat(event.actorId()).isEqualTo("player-1");
        assertThat(event.payload()).contains("durationMs");
        assertThat(event.payload()).contains("\"success\":true");
    }

    @Test
    void timed_error_publishesFailureEvent() {
        RuntimeException boom = new RuntimeException("network error");
        Mono<String> source = Mono.error(boom);

        StepVerifier.create(eventBus.timed(AppEventType.LOGIN_TIMED, "player-1", "player-1", "AUTH", source))
                .expectError(RuntimeException.class)
                .verify();

        assertThat(eventBus.getPublishedEvents()).hasSize(1);
        AppEvent event = eventBus.getPublishedEvents().get(0);
        assertThat(event.eventType()).isEqualTo(AppEventType.LOGIN_TIMED);
        assertThat(event.success()).isFalse();
        assertThat(event.failureReason()).isEqualTo("network error");
        assertThat(event.payload()).contains("\"success\":false");
    }

    @Test
    void timed_doesNotSwallowOriginalValue() {
        Mono<Integer> source = Mono.just(42);

        StepVerifier.create(eventBus.timed(AppEventType.GAME_JOIN_TIMED, "p1", "g1", "GAME", source))
                .assertNext(v -> assertThat(v).isEqualTo(42))
                .verifyComplete();
    }

    @Test
    void timed_doesNotSwallowOriginalError() {
        IllegalStateException cause = new IllegalStateException("unexpected state");
        Mono<String> source = Mono.error(cause);

        StepVerifier.create(eventBus.timed(AppEventType.RANK_CALCULATION_TIMED, "p1", "s1", "RANK", source))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && "unexpected state".equals(e.getMessage()))
                .verify();
    }

    @Test
    void timed_recordsDurationMs() {
        Mono<String> source = Mono.just("done");

        StepVerifier.create(eventBus.timed(AppEventType.GAME_CREATION_TIMED, "p1", "g1", "GAME", source))
                .expectNext("done")
                .verifyComplete();

        AppEvent event = eventBus.getPublishedEvents().get(0);
        assertThat(event.payload()).contains("\"durationMs\":");
        // durationMs >= 0
        assertThat(event.payload()).doesNotContain("\"durationMs\":-");
    }
}
