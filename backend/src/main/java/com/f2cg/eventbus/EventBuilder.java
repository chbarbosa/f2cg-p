package com.f2cg.eventbus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

public class EventBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules();

    private final AppEventType eventType;
    private String actorId;
    private String targetId;
    private String targetType;
    private boolean success;
    private String failureReason;
    private String payload;

    private EventBuilder(AppEventType eventType) {
        this.eventType = eventType;
    }

    public static EventBuilder create(AppEventType eventType) {
        return new EventBuilder(eventType);
    }

    public EventBuilder actor(String actorId) {
        this.actorId = actorId;
        return this;
    }

    public EventBuilder target(String targetId, String targetType) {
        this.targetId = targetId;
        this.targetType = targetType;
        return this;
    }

    public EventBuilder success() {
        this.success = true;
        this.failureReason = null;
        return this;
    }

    public EventBuilder failure(String reason) {
        this.success = false;
        this.failureReason = reason;
        return this;
    }

    public EventBuilder payload(Object payloadObject) {
        if (payloadObject == null) {
            this.payload = null;
            return this;
        }
        try {
            this.payload = MAPPER.writeValueAsString(payloadObject);
        } catch (JsonProcessingException e) {
            this.payload = payloadObject.toString();
        }
        return this;
    }

    public AppEvent build() {
        return new AppEvent(
                UUID.randomUUID().toString(),
                eventType,
                actorId,
                targetId,
                targetType,
                success,
                failureReason,
                payload,
                LocalDateTime.now()
        );
    }
}