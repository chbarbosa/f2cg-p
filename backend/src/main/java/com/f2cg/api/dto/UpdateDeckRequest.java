package com.f2cg.api.dto;

import java.util.List;

public record UpdateDeckRequest(String name, String theme, List<String> cardIds) {}