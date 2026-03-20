package com.f2cg.domain.player;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("players")
public class Player implements Persistable<String> {

    @Id
    private String id;
    private String username;
    @Column("password_hash")
    private String passwordHash;
    private boolean active;
    @Column("activation_code")
    private String activationCode;
    @Column("activation_code_expires")
    private LocalDateTime activationCodeExpires;
    private String nickname;
    private String country;

    @Transient
    private boolean isNew;

    public Player() {
        this.isNew = false;
    }

    public Player(String id, String username, String passwordHash,
                  boolean active, String activationCode, LocalDateTime activationCodeExpires,
                  String nickname, String country) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.active = active;
        this.activationCode = activationCode;
        this.activationCodeExpires = activationCodeExpires;
        this.nickname = nickname;
        this.country = country;
        this.isNew = true;
    }

    @Override
    public boolean isNew() { return isNew; }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActive() { return active; }
    public String getActivationCode() { return activationCode; }
    public LocalDateTime getActivationCodeExpires() { return activationCodeExpires; }

    public void setId(String id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setActive(boolean active) { this.active = active; }
    public void setActivationCode(String activationCode) { this.activationCode = activationCode; }
    public void setActivationCodeExpires(LocalDateTime activationCodeExpires) { this.activationCodeExpires = activationCodeExpires; }
    public String getNickname() { return nickname; }
    public String getCountry() { return country; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setCountry(String country) { this.country = country; }
}