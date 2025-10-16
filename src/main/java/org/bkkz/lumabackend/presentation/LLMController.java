package org.bkkz.lumabackend.presentation;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.bkkz.lumabackend.model.llm.LLMPromptRequest;
import org.bkkz.lumabackend.model.llm.llmResponse.DecoratedItem;
import org.bkkz.lumabackend.model.llm.llmResponse.LLMPromptResponse;
import org.bkkz.lumabackend.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final GoogleCalendarService googleCalendarService;

    @Autowired
    public LLMController(WebClient llmWebClient, TaskService taskService, LogService logService, FormService formService, GoogleCalendarService googleCalendarService) {
        this.googleCalendarService = googleCalendarService;
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
            googleCalendarService.syncGoogleCalendar();
            LLMPromptResponse response = llmWebClient.post()
                    .uri("https://luma-model-local.bkkz.org/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(llmPromptRequest)
                    .retrieve()
                    .bodyToMono(LLMPromptResponse.class)
                    .block();

            if (response == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error","Received an empty response from the LLM service."));
            }

            Map<String, Object> finalResponse = new HashMap<>();

            StringBuilder responseMessages = new StringBuilder();
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
                        for (Map<String, Object> res : serviceResponse.get("results")) {
                            if (res.containsKey("message")) {
                                responseMessages.append(res.get("message")).append(" ");
                            }
                        }
                    }
                    if (serviceResponse.containsKey("errors")) {
                        errorsList.addAll(serviceResponse.get("errors"));
                        for (Map<String, Object> err : serviceResponse.get("errors")) {
                            if (err.containsKey("message")) {
                                responseMessages.append(err.get("message")).append(" ");
                            }
                        }
                    }
                }
            }
            System.out.println("Decorated Response: " + responseMessages);
            finalResponse.put("result", responseMessages.toString());
            logService.createLog(llmPromptRequest.getText(), responseMessages.toString(), allIntents);

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
