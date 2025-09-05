package org.bkkz.lumabackend.model.llm.llmResponse;

import java.util.List;

public record DecoratedInput(
        List<DecoratedItem> decorated,
        String text
) {}
