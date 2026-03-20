package com.f2cg.infrastructure.r2dbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("queue_entries")
public class QueueEntryEntity implements Persistable<String> {

    @Id
    private String id;
    @Column("player_id")
    private String playerId;
    @Column("deck_id")
    private String deckId;
    private String status;
    @Column("joined_at")
    private LocalDateTime joinedAt;

    @Transient
    private boolean isNew;

    public QueueEntryEntity() {
        this.isNew = false;
    }

    public QueueEntryEntity(String id, String playerId, String deckId,
                            String status, LocalDateTime joinedAt) {
        this.id = id;
        this.playerId = playerId;
        this.deckId = deckId;
        this.status = status;
        this.joinedAt = joinedAt;
        this.isNew = true;
    }

    @Override
    public boolean isNew() { return isNew; }

    public String getId() { return id; }
    public String getPlayerId() { return playerId; }
    public String getDeckId() { return deckId; }
    public String getStatus() { return status; }
    public LocalDateTime getJoinedAt() { return joinedAt; }

    public void setId(String id) { this.id = id; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public void setDeckId(String deckId) { this.deckId = deckId; }
    public void setStatus(String status) { this.status = status; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}