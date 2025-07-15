package org.bkkz.lumabackend.presentation;

import com.google.firebase.database.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.bkkz.lumabackend.model.Task;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/task")
public class TaskController {
    @PostMapping(value = "/create-task")
    public ResponseEntity<?> createTask(@Valid @RequestBody Task task, BindingResult bindingResult) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        task.setUserId(userId);

        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(e -> e.getField() + " " + e.getDefaultMessage())
                    .toList();
            return ResponseEntity.badRequest().body(Map.of("errors", errors));
        }
        try {
            DatabaseReference reference = FirebaseDatabase.getInstance()
                    .getReference("tasks")
                    .push();

            reference.setValueAsync(Map.of(
                    "userId", task.getUserId(),
                    "name", task.getName(),
                    "description", task.getDescription() != null ? task.getDescription() : "",
                    "dueDate", task.getDueDate() != null ? task.getDueDate() : "",
                    "dueTime", task.getDueTime() != null ? task.getDueTime() : "",
                    "isFinished", task.getIsFinished()
            ));
            return ResponseEntity.ok(Map.of(
                    "message", "Task Created successfully!",
                    "taskId", reference.getKey()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping(value = "my-tasks")
    public ResponseEntity<?> getMyTasks() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        DatabaseReference reference = FirebaseDatabase.getInstance()
                .getReference("tasks");

        Query query = reference.orderByChild("userId").equalTo(userId);

        CompletableFuture<ResponseEntity<?>> future = new CompletableFuture<>();

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Map<String, Object>> tasks = new ArrayList<>();

                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Map<String, Object> taskData = (Map<String, Object>) child.getValue();
                    taskData.put("id", child.getKey());
                    tasks.add(taskData);
                }

                future.complete(ResponseEntity.ok(tasks));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.complete(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", databaseError.getMessage())));
            }
        });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }

    }
}
