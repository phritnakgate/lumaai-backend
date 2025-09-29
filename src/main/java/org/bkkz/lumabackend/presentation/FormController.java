package org.bkkz.lumabackend.presentation;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.bkkz.lumabackend.service.FormService;
import org.bkkz.lumabackend.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/form")
public class FormController {

    private final TaskService taskService;
    private final FormService formService;
    @Autowired
    public FormController(TaskService taskService, FormService formService) {

        this.taskService = taskService;
        this.formService = formService;
    }

    @PostMapping(path = {"/generate-mis-task-report"}, produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public ResponseEntity<?> generateMisTaskReport(@RequestParam String reportYearMonth) {
        try{
            List<Map<String, Object>> tasks = taskService.getTasksByDate(reportYearMonth).get();

            byte[] report = formService.getMisTaskReport(reportYearMonth, tasks);
            ByteArrayResource resource = new ByteArrayResource(report);

            return ResponseEntity.ok()
                    .contentLength(report.length)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        }catch(Exception e){
            return ResponseEntity.internalServerError()
                    .body(e.getMessage());
        }

    }
}
