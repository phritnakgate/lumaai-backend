package org.bkkz.lumabackend.presentation;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.bkkz.lumabackend.model.task.CreateTaskRequest;
import org.bkkz.lumabackend.model.task.UpdateTaskRequest;
import org.bkkz.lumabackend.service.GoogleCalendarService;
import org.bkkz.lumabackend.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/task")
public class TaskController {

    private final TaskService taskService;
    private final GoogleCalendarService googleCalendarService;

    @Autowired
    public TaskController(TaskService taskService, GoogleCalendarService googleCalendarService) {
        this.googleCalendarService = googleCalendarService;
        this.taskService = taskService;
    }


    @PostMapping(value = "/")
    public ResponseEntity<?> createTask(@Valid @RequestBody CreateTaskRequest task) {

        if (task.getName() == null || task.getName().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Task Name is required"));
        }
        if(!task.getDueDate().isEmpty() && !task.getDueDate().matches("^(\\d{4})-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Due Date must be in yyyy-MM-dd format"));
        }
        if(!task.getDueTime().isEmpty() && !task.getDueTime().matches("^([01]\\d|2[0-3]):([0-5]\\d)$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Due Time must be in HH:mm and 24hr format"));
        }
        if(task.getCategory() < 0 || task.getCategory() > 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid category"));
        }
        if(task.getPriority() < 0 || task.getPriority() > 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid priority"));
        }
        try {
            taskService.createTask(task, false, null, null);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("result", "Task Created successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping(value = "my-tasks")
    public ResponseEntity<?> getMyTasks(@RequestParam(required = false) String date) {
        if(date != null && (!(date.matches("^\\d{4}-\\d{2}-\\d{2}$") || date.matches("^\\d{4}-\\d{2}$")))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Date must be in yyyy-MM-dd or yyyy-MM format"));
        }
        try {
            googleCalendarService.syncGoogleCalendar();
            List<Map<String, Object>> tasks = taskService.getTasksByDate(date).get();
            if (tasks.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(Map.of("result", "No tasks found."));
            } else {
                return ResponseEntity.ok(Map.of("results", tasks));
            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }

    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable String taskId) {
        try {
            CompletableFuture<ResponseEntity<?>> future = taskService.deleteTask(taskId, null);
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{taskId}")
    public ResponseEntity<?> updateTask(
            @PathVariable String taskId,
            @RequestBody @Valid UpdateTaskRequest updateTaskRequest
    ) {
        try {
            CompletableFuture<ResponseEntity<?>> future = taskService.updateTask(taskId, updateTaskRequest, null);
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

}
