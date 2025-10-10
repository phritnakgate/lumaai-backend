package org.bkkz.lumabackend.utils;

import org.jetbrains.annotations.NotNull;

public enum LLMIntent {
    ADD,
    CHECK,
    EDIT,
    REMOVE,
    SEARCH,
    GOOGLESEARCH,
    PLAN,
    GENFORM,
    UNKNOWN;

    public static LLMIntent fromString(@NotNull String intent) {
        try {
            return LLMIntent.valueOf(intent.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LLMIntent.UNKNOWN;
        }
    }
}
