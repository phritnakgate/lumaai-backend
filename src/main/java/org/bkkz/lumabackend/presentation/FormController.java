package org.bkkz.lumabackend.presentation;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.bkkz.lumabackend.service.FormService;
import org.bkkz.lumabackend.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping(path = {"/generate-monthly-task-report"})
    public ResponseEntity<?> generateMonthlyTaskReport(@RequestParam String reportYearMonth) {
        try{
            String uid = getCurrentUserId();
            List<Map<String, Object>> tasks = taskService.getTasksByDate(reportYearMonth).get();

            byte[] report = formService.getMonthlyTaskReport(reportYearMonth, tasks);
            InputStream inputStream = new ByteArrayInputStream(report);

            String reportType = "monthly_task_report";
            String downloadUrl = formService.uploadPdfFile(uid, inputStream, reportType, reportYearMonth);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", downloadUrl);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);

//            return ResponseEntity.ok()
//                    .body(Map.of("result",downloadUrl));
        }catch(Exception e){
            return ResponseEntity.internalServerError()
                    .body(e.getMessage());
        }

    }

    @GetMapping(path = "/forms")
    public ResponseEntity<?> getForms(@RequestParam String formType) {
        try{
            String uid = getCurrentUserId();
            List<Map<String, Object>> forms = formService.getAllUserReports(uid, formType);
            return ResponseEntity.ok()
                    .body(Map.of("results",forms));
        }catch(Exception e){
            return ResponseEntity.internalServerError()
                    .body(e.getMessage());
        }
    }
}
