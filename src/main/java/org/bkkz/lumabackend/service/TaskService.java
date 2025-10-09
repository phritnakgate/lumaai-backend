package org.bkkz.lumabackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.*;
import jakarta.annotation.Nullable;
import org.bkkz.lumabackend.model.task.CreateTaskRequest;
import org.bkkz.lumabackend.model.task.UpdateTaskRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public void createTask(CreateTaskRequest task, boolean isGoogleCalendarTask, String eventId, String uidFromCalendar) {
        String userId = isGoogleCalendarTask ? uidFromCalendar : getCurrentUserId();
        DatabaseReference reference = isGoogleCalendarTask ?
                FirebaseDatabase.getInstance().getReference("tasks").child(eventId) :
                FirebaseDatabase.getInstance().getReference("tasks").push();

        ZonedDateTime dtNow = ZonedDateTime.now(ZoneId.of("GMT+7"));
        LocalDate taskDate = StringUtils.hasText(task.getDueDate()) ? LocalDate.parse(task.getDueDate()) : dtNow.toLocalDate();
        LocalTime taskTime = StringUtils.hasText(task.getDueTime()) ? LocalTime.parse(task.getDueTime()) : dtNow.toLocalTime();
        ZonedDateTime taskDateTime = ZonedDateTime.of(taskDate, taskTime, ZoneId.of("GMT+7"));

        reference.setValueAsync(Map.of(
                "userId", userId,
                "name", task.getName(),
                "description", task.getDescription() != null ? task.getDescription() : "",
                "dateTime", taskDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                "isFinished", false,
                "category", task.getCategory(),
                "priority", task.getPriority(),
                "isGoogleCalendarTask",isGoogleCalendarTask
        ));
    }

    public CompletableFuture<List<Map<String, Object>>> getTasksByDate(@Nullable String date) {
        String userId = getCurrentUserId();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("tasks");
        Query query = reference.orderByChild("userId").equalTo(userId);
        CompletableFuture<List<Map<String, Object>>> future = new CompletableFuture<>();

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Map<String, Object>> tasks = new ArrayList<>();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Map<String, Object> taskData = (Map<String, Object>) child.getValue();
                    if (taskData != null) {
                        taskData.put("id", child.getKey());
                        tasks.add(taskData);
                    }
                }

                if (date == null || date.isEmpty()) {
                    future.complete(tasks);
                } else {
                    List<Map<String, Object>> filteredTasks = tasks.stream()
                            .filter(task -> ((String) task.get("dateTime")).startsWith(date))
                            .collect(Collectors.toList());
                    future.complete(filteredTasks);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.obtrudeException(databaseError.toException());
            }
        });
        return future;
    }


    public CompletableFuture<ResponseEntity<?>> deleteTask(String taskId, String uidFromCalendar){
        String userId = (uidFromCalendar == null) ? getCurrentUserId() : uidFromCalendar;
        DatabaseReference taskRef = FirebaseDatabase.getInstance()
                .getReference("tasks")
                .child(taskId);

        CompletableFuture<ResponseEntity<?>> future = new CompletableFuture<>();

        taskRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (checkTaskValidity(snapshot, future, userId)) return;

                taskRef.removeValueAsync();
                future.complete(ResponseEntity.status(HttpStatus.NO_CONTENT).body(Map.of("result", "Task Deleted successfully!")));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.complete(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", error.getMessage())));
            }
        });
        return future;
    }

    public CompletableFuture<ResponseEntity<?>> updateTask(String taskId, UpdateTaskRequest updateTaskRequest, String uidFromCalendar){
        String userId = (uidFromCalendar == null) ? getCurrentUserId() : uidFromCalendar;
        DatabaseReference taskRef = FirebaseDatabase.getInstance()
                .getReference("tasks")
                .child(taskId);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> updates = objectMapper.convertValue(updateTaskRequest, Map.class);

        updates.entrySet().removeIf(entry -> entry.getValue() == null);

        CompletableFuture<ResponseEntity<?>> future = new CompletableFuture<>();

        taskRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (checkTaskValidity(snapshot, future, userId)) return;

                if (updates.isEmpty()) {
                    future.complete(ResponseEntity.badRequest().body(Map.of("error", "No fields to update or invalid data provided.")));
                    return;
                }

                taskRef.updateChildrenAsync(updates);
                future.complete(ResponseEntity.ok(Map.of("result", "Task updated successfully")));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.complete(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", error.getMessage())));
            }
        });
        return future;
    }

    private boolean checkTaskValidity(@NotNull DataSnapshot snapshot, CompletableFuture<ResponseEntity<?>> future, String userId) {
        if (!snapshot.exists()) {
            future.complete(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Task with this id not found.")));
            return true;
        }

        String ownerId = snapshot.child("userId").getValue(String.class);
        if (!userId.equals(ownerId)) {
            future.complete(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You do not have permission to modify this task.")));
            return true;
        }
        return false;
    }

}
