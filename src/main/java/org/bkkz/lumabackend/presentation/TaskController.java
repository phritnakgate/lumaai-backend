package org.bkkz.lumabackend.presentation;

import com.google.firebase.database.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.bkkz.lumabackend.model.CreateTaskRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/task")
public class TaskController {
    @PostMapping(value = "/")
    public ResponseEntity<?> createTask(@Valid @RequestBody CreateTaskRequest task) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        if (task.getName() == null || task.getName().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Task Name is required"));
        }
        if(!task.getDueDate().isEmpty() && !task.getDueDate().matches("^(\\d{4})-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Due Date must be in yyyy-MM-dd format"));
        }
        if(!task.getDueTime().isEmpty() && !task.getDueTime().matches("^([01]\\d|2[0-3]):([0-5]\\d)$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Due Time must be in HH:mm and 24hr format"));
        }
        try {
            DatabaseReference reference = FirebaseDatabase.getInstance()
                    .getReference("tasks")
                    .push();

            ZonedDateTime dtNow = ZonedDateTime.now(ZoneId.of("GMT+7"));
            ZonedDateTime taskDateTime;
            try {
                LocalDate taskDate = StringUtils.hasText(task.getDueDate()) ? LocalDate.parse(task.getDueDate()) : dtNow.toLocalDate();
                LocalTime taskTime = StringUtils.hasText(task.getDueTime()) ? LocalTime.parse(task.getDueTime()) : dtNow.toLocalTime();
                taskDateTime = ZonedDateTime.of(taskDate, taskTime, ZoneId.of("GMT+7"));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid date or time"));
            }

            reference.setValueAsync(Map.of(
                    "userId", userId,
                    "name", task.getName(),
                    "description", task.getDescription() != null ? task.getDescription() : "",
                    "dateTime", taskDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    "isFinished", false
            ));
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "result", "Task Created successfully!"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping(value = "my-tasks")
    public ResponseEntity<?> getMyTasks(@RequestParam(required = false) String date) {
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
                    if(taskData != null){
                        taskData.put("id", child.getKey());
                        tasks.add(taskData);
                    }

                }

                List<Map<String, Object>> filteredTasks;
                if (!StringUtils.hasText(date)) {
                    filteredTasks = tasks;
                } else {
                    if(!(date.matches("^\\d{4}-\\d{2}-\\d{2}$") || date.matches("^\\d{4}-\\d{2}$"))) {
                        future.complete(ResponseEntity.badRequest().body(Map.of("error", "Date must be in yyyy-MM-dd or yyyy-MM format")));
                        return;
                    }
                    filteredTasks = tasks.stream().filter(task -> {
                        Object dateTimeObj = task.get("dateTime");
                        if (!(dateTimeObj instanceof String taskDateTime)) {
                            return false;
                        }
                        return taskDateTime.startsWith(date);
                    }).collect(Collectors.toList());
                }
                if (filteredTasks.isEmpty()) {
                    future.complete(ResponseEntity.status(HttpStatus.NO_CONTENT).body(Map.of("result", "No tasks found.")));
                } else {
                    future.complete(ResponseEntity.status(HttpStatus.OK).body(Map.of("results",filteredTasks)));
                }
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
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }

    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable String taskId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        DatabaseReference taskRef = FirebaseDatabase.getInstance()
                .getReference("tasks")
                .child(taskId);

        CompletableFuture<ResponseEntity<?>> future = new CompletableFuture<>();

        taskRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    future.complete(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Task with this id not found.")));
                    return;
                }

                String ownerId = snapshot.child("userId").getValue(String.class);
                if (!userId.equals(ownerId)) {
                    future.complete(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("error", "You do not have permission to delete this task.")));
                    return;
                }

                taskRef.removeValueAsync();
                future.complete(ResponseEntity.status(HttpStatus.NO_CONTENT).body(Map.of("result", "Task Deleted successfully!")));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.complete(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", error.getMessage())));
            }
        });

        try {
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
            @RequestBody Map<String, Object> updates
    ) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        DatabaseReference taskRef = FirebaseDatabase.getInstance()
                .getReference("tasks")
                .child(taskId);

        CompletableFuture<ResponseEntity<?>> future = new CompletableFuture<>();

        taskRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    future.complete(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Task not found")));
                    return;
                }

                String ownerId = snapshot.child("userId").getValue(String.class);
                if (!userId.equals(ownerId)) {
                    future.complete(ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "You do not have permission to update this task")));
                    return;
                }

                taskRef.updateChildrenAsync(updates);
                future.complete(ResponseEntity.ok(Map.of("message", "Task updated successfully")));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.complete(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", error.getMessage())));
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
