package org.bkkz.lumabackend.model.llm.llmResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LLMPromptResponse(
        @JsonProperty("decorated_input") DecoratedInput decoratedInput,
        String text,
        String message
) {}
