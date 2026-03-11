package com.f2cg.domain.player;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("players")
public class Player implements Persistable<String> {

    @Id
    private String id;
    private String username;
    @Column("password_hash")
    private String passwordHash;

    @Transient
    private boolean isNew;

    public Player() {
        this.isNew = false;
    }

    public Player(String id, String username, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.isNew = true;
    }

    @Override
    public boolean isNew() { return isNew; }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }

    public void setId(String id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}