package org.bkkz.lumabackend.model.llm.llmResponse;

import java.util.List;

public record DecoratedItem(
        List<String> intent,
        String task,
        String date,
        String time,
        String response,
        String source
) {}
