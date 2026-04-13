package com.f2cg.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class InMemoryEventBus implements EventBus {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEventBus.class);

    // TODO: replace with Kafka/RabbitMQ producer in production
    // Events are logged and stored in memory for dev/test inspection
    private final List<AppEvent> publishedEvents =
            Collections.synchronizedList(new ArrayList<>());

    @Override
    public void publish(AppEvent event) {
        publishedEvents.add(event);
        log.info("[EVENT BUS] type={} actorId={} targetId={} occurredAt={}",
                event.eventType(), event.actorId(), event.targetId(), event.occurredAt());
    }

    public List<AppEvent> getPublishedEvents() {
        return Collections.unmodifiableList(publishedEvents);
    }

    public void clear() {
        publishedEvents.clear();
    }
}
