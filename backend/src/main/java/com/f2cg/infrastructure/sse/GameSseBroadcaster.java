package com.f2cg.infrastructure.sse;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.FluxSink;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameSseBroadcaster {

    private final Map<String, FluxSink<ServerSentEvent<Object>>> sinks = new ConcurrentHashMap<>();

    public void register(String playerId, FluxSink<ServerSentEvent<Object>> sink) {
        sinks.put(playerId, sink);
    }

    public void emit(String playerId, String eventName, Object payload) {
        FluxSink<ServerSentEvent<Object>> sink = sinks.get(playerId);
        if (sink != null) {
            sink.next(ServerSentEvent.builder(payload)
                    .event(eventName)
                    .build());
        }
    }

    public void complete(String playerId) {
        FluxSink<ServerSentEvent<Object>> sink = sinks.remove(playerId);
        if (sink != null) {
            sink.complete();
        }
    }

    public void remove(String playerId) {
        sinks.remove(playerId);
    }
}