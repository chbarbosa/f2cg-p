package com.f2cg.infrastructure.r2dbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("decks")
public class DeckEntity implements Persistable<String> {

    @Id
    private String id;
    @Column("player_id")
    private String playerId;
    private String name;
    private String theme;
    @Column("card_ids")
    private String cardIds;
    private String status;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private boolean isNew;

    public DeckEntity() {
        this.isNew = false;
    }

    public DeckEntity(String id, String playerId, String name, String theme,
                      String cardIds, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.playerId = playerId;
        this.name = name;
        this.theme = theme;
        this.cardIds = cardIds;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isNew = true;
    }

    @Override
    public boolean isNew() { return isNew; }

    public String getId() { return id; }
    public String getPlayerId() { return playerId; }
    public String getName() { return name; }
    public String getTheme() { return theme; }
    public String getCardIds() { return cardIds; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(String id) { this.id = id; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public void setName(String name) { this.name = name; }
    public void setTheme(String theme) { this.theme = theme; }
    public void setCardIds(String cardIds) { this.cardIds = cardIds; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}