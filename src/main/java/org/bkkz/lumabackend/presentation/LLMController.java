package org.bkkz.lumabackend.presentation;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.bkkz.lumabackend.model.llm.llmResponse.DecoratedItem;
import org.bkkz.lumabackend.model.llm.LLMPromptRequest;
import org.bkkz.lumabackend.model.llm.llmResponse.LLMPromptResponse;
import org.bkkz.lumabackend.service.LLMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequestMapping("/api/llm")
public class LLMController {

    private final WebClient llmWebClient;

    @Autowired
    public LLMController(WebClient llmWebClient) {
        this.llmWebClient = llmWebClient;
    }

    @PostMapping("/")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> handleLLMRequest(@Valid @RequestBody LLMPromptRequest llmPromptRequest) {
        //Call an external LLM service
        try{
            LLMPromptResponse response = llmWebClient.post()
                    .uri("/handle-input")
                    .retrieve()
                    .bodyToMono(LLMPromptResponse.class)
                    .block();

            for(DecoratedItem item : response.decoratedInput().decorated()) {
                System.out.println(item);
                LLMService llmService = new LLMService(item);
                llmService.processIntent();
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
