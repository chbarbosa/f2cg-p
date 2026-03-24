package com.f2cg.api;

import com.f2cg.api.dto.PerformanceResponse;
import com.f2cg.api.dto.SeasonSummaryResponse;
import com.f2cg.application.PerformanceService;
import com.f2cg.infrastructure.JwtUtil;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    private final PerformanceService performanceService;
    private final JwtUtil jwtUtil;

    public PerformanceController(PerformanceService performanceService, JwtUtil jwtUtil) {
        this.performanceService = performanceService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/current")
    public Mono<PerformanceResponse> getCurrent(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String playerId = jwtUtil.extractPlayerIdFromHeader(auth);
        return performanceService.getCurrentPerformance(playerId);
    }

    @GetMapping
    public Mono<PerformanceResponse> getBySeason(
            @RequestParam String seasonId,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String playerId = jwtUtil.extractPlayerIdFromHeader(auth);
        return performanceService.getSeasonPerformance(playerId, seasonId);
    }

    @GetMapping("/seasons")
    public Flux<SeasonSummaryResponse> getSeasons(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String playerId = jwtUtil.extractPlayerIdFromHeader(auth);
        return performanceService.getParticipatedSeasons(playerId);
    }
}