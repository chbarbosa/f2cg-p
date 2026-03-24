package com.f2cg.infrastructure.r2dbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("player_season_stats")
public class PlayerSeasonStatsEntity implements Persistable<String> {

    @Id
    private String id;
    @Column("player_id")
    private String playerId;
    @Column("season_id")
    private String seasonId;
    @Column("total_matches")
    private int totalMatches;
    private int victories;
    private int defeats;
    private String rank;
    @Column("highest_rank")
    private String highestRank;
    @Column("matches_this_week")
    private int matchesThisWeek;
    @Column("last_rank_update")
    private LocalDateTime lastRankUpdate;

    @Transient
    private boolean isNew;

    public PlayerSeasonStatsEntity() {
        this.isNew = false;
    }

    public PlayerSeasonStatsEntity(String id, String playerId, String seasonId,
                                   int totalMatches, int victories, int defeats,
                                   String rank, String highestRank,
                                   int matchesThisWeek, LocalDateTime lastRankUpdate) {
        this.id = id;
        this.playerId = playerId;
        this.seasonId = seasonId;
        this.totalMatches = totalMatches;
        this.victories = victories;
        this.defeats = defeats;
        this.rank = rank;
        this.highestRank = highestRank;
        this.matchesThisWeek = matchesThisWeek;
        this.lastRankUpdate = lastRankUpdate;
        this.isNew = true;
    }

    @Override
    public boolean isNew() { return isNew; }

    public String getId() { return id; }
    public String getPlayerId() { return playerId; }
    public String getSeasonId() { return seasonId; }
    public int getTotalMatches() { return totalMatches; }
    public int getVictories() { return victories; }
    public int getDefeats() { return defeats; }
    public String getRank() { return rank; }
    public String getHighestRank() { return highestRank; }
    public int getMatchesThisWeek() { return matchesThisWeek; }
    public LocalDateTime getLastRankUpdate() { return lastRankUpdate; }

    public void setId(String id) { this.id = id; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public void setSeasonId(String seasonId) { this.seasonId = seasonId; }
    public void setTotalMatches(int totalMatches) { this.totalMatches = totalMatches; }
    public void setVictories(int victories) { this.victories = victories; }
    public void setDefeats(int defeats) { this.defeats = defeats; }
    public void setRank(String rank) { this.rank = rank; }
    public void setHighestRank(String highestRank) { this.highestRank = highestRank; }
    public void setMatchesThisWeek(int matchesThisWeek) { this.matchesThisWeek = matchesThisWeek; }
    public void setLastRankUpdate(LocalDateTime lastRankUpdate) { this.lastRankUpdate = lastRankUpdate; }
}