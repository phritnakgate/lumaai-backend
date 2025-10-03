package org.bkkz.lumabackend.presentation;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.bkkz.lumabackend.model.llm.LLMPromptRequest;
import org.bkkz.lumabackend.model.llm.llmResponse.DecoratedItem;
import org.bkkz.lumabackend.model.llm.llmResponse.LLMPromptResponse;
import org.bkkz.lumabackend.service.FormService;
import org.bkkz.lumabackend.service.LLMService;
import org.bkkz.lumabackend.service.LogService;
import org.bkkz.lumabackend.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    private final FormService formService;

    @Autowired
    public LLMController(WebClient llmWebClient, TaskService taskService, LogService logService, FormService formService) {
        this.llmWebClient = llmWebClient;
        this.taskService = taskService;
        this.logService = logService;
        this.formService = formService;
    }

    @PostMapping("/")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> handleLLMRequest(@Valid @RequestBody LLMPromptRequest llmPromptRequest) {
        //Call an external LLM service
        try{
            LLMPromptResponse response = llmWebClient.get()
                    .uri("https://jsonblob.com/api/1412324703066054656")
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
                    LLMService llmService = new LLMService(item, taskService, formService);
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getLLMHistory(@RequestParam(required = false) String intent, @RequestParam(required = false) String date, @RequestParam(required = false) String keyword) {
        if(date != null && (!(date.matches("^\\d{4}-\\d{2}-\\d{2}$") || date.matches("^\\d{4}-\\d{2}$")))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Date must be in yyyy-MM-dd or yyyy-MM format"));
        }
        try {
            List<Map<String, Object>> logs = logService.getLogs(intent, date, keyword).get();
            if(logs.isEmpty()){
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(Map.of("result", "No logs found"));
            }else{
                return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                        "results", logs
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

    }
}
