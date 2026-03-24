package com.f2cg.infrastructure.r2dbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Table("seasons")
public class SeasonEntity implements Persistable<String> {

    @Id
    private String id;
    @Column("season_year")
    private int year;
    @Column("season_number")
    private int seasonNumber;
    private String name;
    @Column("start_date")
    private LocalDate startDate;
    @Column("end_date")
    private LocalDate endDate;
    @Column("phase2_start_date")
    private LocalDate phase2StartDate;
    private String status;
    @Column("last_weekly_calculation")
    private LocalDate lastWeeklyCalculation;

    @Transient
    private boolean isNew;

    public SeasonEntity() {
        this.isNew = false;
    }

    public SeasonEntity(String id, int year, int seasonNumber, String name,
                        LocalDate startDate, LocalDate endDate, LocalDate phase2StartDate,
                        String status, LocalDate lastWeeklyCalculation) {
        this.id = id;
        this.year = year;
        this.seasonNumber = seasonNumber;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.phase2StartDate = phase2StartDate;
        this.status = status;
        this.lastWeeklyCalculation = lastWeeklyCalculation;
        this.isNew = true;
    }

    @Override
    public boolean isNew() { return isNew; }

    public String getId() { return id; }
    public int getYear() { return year; }
    public int getSeasonNumber() { return seasonNumber; }
    public String getName() { return name; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalDate getPhase2StartDate() { return phase2StartDate; }
    public String getStatus() { return status; }
    public LocalDate getLastWeeklyCalculation() { return lastWeeklyCalculation; }

    public void setId(String id) { this.id = id; }
    public void setYear(int year) { this.year = year; }
    public void setSeasonNumber(int seasonNumber) { this.seasonNumber = seasonNumber; }
    public void setName(String name) { this.name = name; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setPhase2StartDate(LocalDate phase2StartDate) { this.phase2StartDate = phase2StartDate; }
    public void setStatus(String status) { this.status = status; }
    public void setLastWeeklyCalculation(LocalDate lastWeeklyCalculation) { this.lastWeeklyCalculation = lastWeeklyCalculation; }
}