package com.f2cg.eventbus;

import reactor.core.publisher.Mono;

public interface EventBus {

    void publish(AppEvent event);

    default <T> Mono<T> timed(AppEventType eventType,
                               String actorId,
                               String targetId,
                               String targetType,
                               Mono<T> source) {
        return Mono.defer(() -> {
            long start = System.currentTimeMillis();
            return source
                    .doOnSuccess(v -> publish(EventBuilder.create(eventType)
                            .actor(actorId)
                            .target(targetId, targetType)
                            .success()
                            .payload(new TimedPayload(eventType.name(),
                                    System.currentTimeMillis() - start, true))
                            .build()))
                    .doOnError(e -> publish(EventBuilder.create(eventType)
                            .actor(actorId)
                            .target(targetId, targetType)
                            .failure(e.getMessage())
                            .payload(new TimedPayload(eventType.name(),
                                    System.currentTimeMillis() - start, false))
                            .build()));
        });
    }
}