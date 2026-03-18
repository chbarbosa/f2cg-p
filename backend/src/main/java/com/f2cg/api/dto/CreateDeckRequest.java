package com.f2cg.api.dto;

import java.util.List;

public record CreateDeckRequest(String name, String theme, List<String> cardIds) {}