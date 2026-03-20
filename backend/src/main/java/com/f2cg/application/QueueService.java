package com.f2cg.application;

import com.f2cg.domain.deck.DeckStatus;
import com.f2cg.domain.queue.QueueEntry;
import com.f2cg.domain.queue.QueueStatus;
import com.f2cg.infrastructure.r2dbc.DeckRepository;
import com.f2cg.infrastructure.r2dbc.QueueEntryEntity;
import com.f2cg.infrastructure.r2dbc.QueueEntryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class QueueService {

    private final QueueEntryRepository queueEntryRepository;
    private final DeckRepository deckRepository;

    public QueueService(QueueEntryRepository queueEntryRepository, DeckRepository deckRepository) {
        this.queueEntryRepository = queueEntryRepository;
        this.deckRepository = deckRepository;
    }

    public Mono<QueueEntry> joinQueue(String playerId, String deckId) {
        return deckRepository.findById(deckId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found")))
                .flatMap(deck -> {
                    if (!deck.getPlayerId().equals(playerId)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
                    }
                    if (DeckStatus.DRAFT.name().equals(deck.getStatus())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Deck must be PLAYABLE to join queue"));
                    }
                    return queueEntryRepository
                            .findByPlayerIdAndStatus(playerId, QueueStatus.WAITING.name())
                            .flatMap(existing -> Mono.<QueueEntryEntity>error(
                                    new ResponseStatusException(HttpStatus.CONFLICT, "Already in queue")))
                            .switchIfEmpty(Mono.defer(() -> {
                                QueueEntryEntity entity = new QueueEntryEntity(
                                        UUID.randomUUID().toString(),
                                        playerId,
                                        deckId,
                                        QueueStatus.WAITING.name(),
                                        LocalDateTime.now()
                                );
                                return queueEntryRepository.save(entity);
                            }));
                })
                .map(this::toDomain);
    }

    public Mono<QueueEntry> cancelQueue(String playerId) {
        return queueEntryRepository.findByPlayerIdAndStatus(playerId, QueueStatus.WAITING.name())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active queue entry")))
                .flatMap(entity -> {
                    entity.setStatus(QueueStatus.CANCELLED.name());
                    return queueEntryRepository.save(entity);
                })
                .map(this::toDomain);
    }

    public Mono<QueueEntry> getStatus(String playerId) {
        return queueEntryRepository
                .findByPlayerIdAndStatusIn(playerId,
                        List.of(QueueStatus.WAITING.name(), QueueStatus.MATCHED.name()))
                .next()
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active queue entry")))
                .map(this::toDomain);
    }

    private QueueEntry toDomain(QueueEntryEntity entity) {
        return new QueueEntry(
                entity.getId(),
                entity.getPlayerId(),
                entity.getDeckId(),
                QueueStatus.valueOf(entity.getStatus()),
                entity.getJoinedAt()
        );
    }
}