package org.bkkz.lumabackend.model.llm;

import jakarta.validation.constraints.NotBlank;

public class LLMPromptRequest {
    @NotBlank(message = "Prompt text must not be blank")
    private String text;

    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
}
