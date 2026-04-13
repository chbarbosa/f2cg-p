package com.f2cg.eventbus;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventBuilderTest {

    @Test
    void create_success_setsCorrectFields() {
        AppEvent event = EventBuilder.create(AppEventType.LOGIN_SUCCESS)
                .actor("player-1")
                .target("player-1", "AUTH")
                .success()
                .build();

        assertThat(event.eventType()).isEqualTo(AppEventType.LOGIN_SUCCESS);
        assertThat(event.actorId()).isEqualTo("player-1");
        assertThat(event.targetId()).isEqualTo("player-1");
        assertThat(event.targetType()).isEqualTo("AUTH");
        assertThat(event.success()).isTrue();
        assertThat(event.failureReason()).isNull();
    }

    @Test
    void create_failure_setsCorrectFields() {
        AppEvent event = EventBuilder.create(AppEventType.LOGIN_FAILURE)
                .actor(null)
                .target(null, "AUTH")
                .failure("Invalid credentials")
                .build();

        assertThat(event.eventType()).isEqualTo(AppEventType.LOGIN_FAILURE);
        assertThat(event.success()).isFalse();
        assertThat(event.failureReason()).isEqualTo("Invalid credentials");
        assertThat(event.actorId()).isNull();
        assertThat(event.targetId()).isNull();
    }

    @Test
    void build_generatesUniquePublicId() {
        AppEvent e1 = EventBuilder.create(AppEventType.LOGIN_SUCCESS).actor("p1").target("p1", "AUTH").success().build();
        AppEvent e2 = EventBuilder.create(AppEventType.LOGIN_SUCCESS).actor("p1").target("p1", "AUTH").success().build();

        assertThat(e1.publicId()).isNotNull();
        assertThat(e2.publicId()).isNotNull();
        assertThat(e1.publicId()).isNotEqualTo(e2.publicId());
    }

    @Test
    void build_setsOccurredAt() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        AppEvent event = EventBuilder.create(AppEventType.USER_UPDATED)
                .actor("p1")
                .target("p1", "USER")
                .success()
                .build();

        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.occurredAt()).isAfter(before);
        assertThat(event.occurredAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    void payload_isSerializedToJson() {
        TimedPayload timedPayload = new TimedPayload("LOGIN", 120L, true);

        AppEvent event = EventBuilder.create(AppEventType.LOGIN_TIMED)
                .actor("p1")
                .target("p1", "AUTH")
                .success()
                .payload(timedPayload)
                .build();

        assertThat(event.payload()).contains("operationName");
        assertThat(event.payload()).contains("LOGIN");
        assertThat(event.payload()).contains("durationMs");
        assertThat(event.payload()).contains("120");
    }

    @Test
    void actor_acceptsNull() {
        AppEvent event = EventBuilder.create(AppEventType.LOGIN_FAILURE)
                .actor(null)
                .target(null, "AUTH")
                .failure("bad")
                .build();

        assertThat(event.actorId()).isNull();
        assertThat(event.publicId()).isNotNull();
    }

    @Test
    void payload_null_setsNullPayload() {
        AppEvent event = EventBuilder.create(AppEventType.LOGIN_SUCCESS)
                .actor("p1")
                .target("p1", "AUTH")
                .success()
                .payload(null)
                .build();

        assertThat(event.payload()).isNull();
    }
}