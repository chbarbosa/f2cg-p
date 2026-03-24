package com.f2cg.api.dto;

import com.f2cg.domain.season.Season;

import java.time.LocalDate;

public record SeasonSummaryResponse(
        String id,
        int year,
        int seasonNumber,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate phase2StartDate
) {
    public static SeasonSummaryResponse from(Season season) {
        return new SeasonSummaryResponse(
                season.id(),
                season.year(),
                season.seasonNumber(),
                season.name(),
                season.startDate(),
                season.endDate(),
                season.phase2StartDate()
        );
    }
}