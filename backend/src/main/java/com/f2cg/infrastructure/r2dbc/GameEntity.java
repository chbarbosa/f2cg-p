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
    @Column("winner_id")
    private String winnerId;
    @Column("player1_heartbeat")
    private LocalDateTime player1Heartbeat;
    @Column("player2_heartbeat")
    private LocalDateTime player2Heartbeat;
    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("player1_hand")
    private String player1Hand;
    @Column("player2_hand")
    private String player2Hand;
    @Column("player1_stack")
    private String player1Stack;
    @Column("player2_stack")
    private String player2Stack;
    @Column("player1_field")
    private String player1Field;
    @Column("player2_field")
    private String player2Field;
    @Column("player1_graveyard")
    private String player1Graveyard;
    @Column("player2_graveyard")
    private String player2Graveyard;
    @Column("player1_summoning_confirmed")
    private boolean player1SummoningConfirmed;
    @Column("player2_summoning_confirmed")
    private boolean player2SummoningConfirmed;

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
        this.player1Hand = "";
        this.player2Hand = "";
        this.player1Stack = "";
        this.player2Stack = "";
        this.player1Field = "";
        this.player2Field = "";
        this.player1Graveyard = "";
        this.player2Graveyard = "";
        this.player1SummoningConfirmed = false;
        this.player2SummoningConfirmed = false;
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
    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
    public LocalDateTime getPlayer1Heartbeat() { return player1Heartbeat; }
    public void setPlayer1Heartbeat(LocalDateTime player1Heartbeat) { this.player1Heartbeat = player1Heartbeat; }
    public LocalDateTime getPlayer2Heartbeat() { return player2Heartbeat; }
    public void setPlayer2Heartbeat(LocalDateTime player2Heartbeat) { this.player2Heartbeat = player2Heartbeat; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getPlayer1Hand() { return player1Hand; }
    public void setPlayer1Hand(String player1Hand) { this.player1Hand = player1Hand; }
    public String getPlayer2Hand() { return player2Hand; }
    public void setPlayer2Hand(String player2Hand) { this.player2Hand = player2Hand; }
    public String getPlayer1Stack() { return player1Stack; }
    public void setPlayer1Stack(String player1Stack) { this.player1Stack = player1Stack; }
    public String getPlayer2Stack() { return player2Stack; }
    public void setPlayer2Stack(String player2Stack) { this.player2Stack = player2Stack; }
    public String getPlayer1Field() { return player1Field; }
    public void setPlayer1Field(String player1Field) { this.player1Field = player1Field; }
    public String getPlayer2Field() { return player2Field; }
    public void setPlayer2Field(String player2Field) { this.player2Field = player2Field; }
    public String getPlayer1Graveyard() { return player1Graveyard; }
    public void setPlayer1Graveyard(String player1Graveyard) { this.player1Graveyard = player1Graveyard; }
    public String getPlayer2Graveyard() { return player2Graveyard; }
    public void setPlayer2Graveyard(String player2Graveyard) { this.player2Graveyard = player2Graveyard; }
    public boolean isPlayer1SummoningConfirmed() { return player1SummoningConfirmed; }
    public void setPlayer1SummoningConfirmed(boolean player1SummoningConfirmed) { this.player1SummoningConfirmed = player1SummoningConfirmed; }
    public boolean isPlayer2SummoningConfirmed() { return player2SummoningConfirmed; }
    public void setPlayer2SummoningConfirmed(boolean player2SummoningConfirmed) { this.player2SummoningConfirmed = player2SummoningConfirmed; }
}