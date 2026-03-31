package com.f2cg.infrastructure.r2dbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("games")
public class GameEntity implements Persistable<Long> {

    @Id
    private Long id;
    @Column("public_id")
    private String publicId;
    @Column("player1_id")
    private String player1Id;
    @Column("player1_public_id")
    private String player1PublicId;
    @Column("player1_username")
    private String player1Username;
    @Column("player2_id")
    private String player2Id;
    @Column("player2_public_id")
    private String player2PublicId;
    @Column("player2_username")
    private String player2Username;
    private String status;
    @Column("created_at")
    private LocalDateTime createdAt;

    @Transient
    private boolean isNew;

    public GameEntity() {
        this.isNew = false;
    }

    public GameEntity(String publicId,
                      String player1Id, String player1PublicId, String player1Username,
                      String player2Id, String player2PublicId, String player2Username,
                      String status, LocalDateTime createdAt) {
        this.publicId = publicId;
        this.player1Id = player1Id;
        this.player1PublicId = player1PublicId;
        this.player1Username = player1Username;
        this.player2Id = player2Id;
        this.player2PublicId = player2PublicId;
        this.player2Username = player2Username;
        this.status = status;
        this.createdAt = createdAt;
        this.isNew = true;
    }

    @Override
    public boolean isNew() { return isNew; }

    public Long getId() { return id; }
    public String getPublicId() { return publicId; }
    public String getPlayer1Id() { return player1Id; }
    public String getPlayer1PublicId() { return player1PublicId; }
    public String getPlayer1Username() { return player1Username; }
    public String getPlayer2Id() { return player2Id; }
    public String getPlayer2PublicId() { return player2PublicId; }
    public String getPlayer2Username() { return player2Username; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public void setPlayer1Id(String player1Id) { this.player1Id = player1Id; }
    public void setPlayer1PublicId(String player1PublicId) { this.player1PublicId = player1PublicId; }
    public void setPlayer1Username(String player1Username) { this.player1Username = player1Username; }
    public void setPlayer2Id(String player2Id) { this.player2Id = player2Id; }
    public void setPlayer2PublicId(String player2PublicId) { this.player2PublicId = player2PublicId; }
    public void setPlayer2Username(String player2Username) { this.player2Username = player2Username; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}