package com.f2cg.domain.season;

import java.time.LocalDate;

public record Season(
        String id,
        int year,
        int seasonNumber,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate phase2StartDate,
        SeasonStatus status,
        LocalDate lastWeeklyCalculation
) {}