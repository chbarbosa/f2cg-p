package com.f2cg.application;

import java.util.Arrays;
import java.util.List;

public final class CardIdConverter {

    private CardIdConverter() {}

    public static List<String> toList(String cardIds) {
        if (cardIds == null || cardIds.isBlank()) return List.of();
        return Arrays.stream(cardIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static String toString(List<String> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) return "";
        return String.join(",", cardIds);
    }
}