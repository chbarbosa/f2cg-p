package com.f2cg.api.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinQueueRequest(
        @NotBlank(message = "deckId is required")
        String deckId
) {}