package org.bkkz.lumabackend.presentation;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.bkkz.lumabackend.model.llm.llmResponse.DecoratedItem;
import org.bkkz.lumabackend.model.llm.LLMPromptRequest;
import org.bkkz.lumabackend.model.llm.llmResponse.LLMPromptResponse;
import org.bkkz.lumabackend.service.LLMService;
import org.bkkz.lumabackend.service.LogService;
import org.bkkz.lumabackend.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/llm")
public class LLMController {

    private final WebClient llmWebClient;
    private final TaskService taskService;
    private final LogService logService;

    @Autowired
    public LLMController(WebClient llmWebClient, TaskService taskService, LogService logService) {
        this.llmWebClient = llmWebClient;
        this.taskService = taskService;
        this.logService = logService;
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

            if (response == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Received an empty response from the LLM service.");
            }

            Map<String, Object> finalResponse = new HashMap<>();

            finalResponse.put("result", response.message());
            List<Map<String, Object>> resultsList = new ArrayList<>();
            finalResponse.put("results", resultsList);
            List<Map<String, Object>> errorsList = new ArrayList<>();
            finalResponse.put("errors", errorsList);

            ArrayList<String> allIntents = new ArrayList<>();

            if (response.decoratedInput() != null) {
                for(DecoratedItem item : response.decoratedInput().decorated()) {
                    System.out.println(item);
                    allIntents.addAll(item.intent());
                    LLMService llmService = new LLMService(item, taskService);
                    Map<String, List<Map<String, Object>>> serviceResponse = llmService.processIntent();
                    if (serviceResponse.containsKey("results")) {
                        resultsList.addAll(serviceResponse.get("results"));
                    }
                    if (serviceResponse.containsKey("errors")) {
                        errorsList.addAll(serviceResponse.get("errors"));
                    }
                }
            }

            logService.createLog(llmPromptRequest.getText(), response.message(), allIntents);

            return ResponseEntity.status(HttpStatus.OK).body(finalResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
