package com.f2cg.eventbus;

public record TimedPayload(
        String operationName,
        Long durationMs,
        boolean success
) {}
