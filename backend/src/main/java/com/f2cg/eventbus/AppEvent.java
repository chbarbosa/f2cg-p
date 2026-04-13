package com.f2cg.eventbus;

import java.time.LocalDateTime;

public record AppEvent(
        String publicId,
        AppEventType eventType,
        String actorId,
        String targetId,
        String targetType,
        boolean success,
        String failureReason,
        String payload,
        LocalDateTime occurredAt
) {}
