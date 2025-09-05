package org.bkkz.lumabackend.service;

import com.google.firebase.database.*;
import jakarta.annotation.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class LogService {

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public void createLog(String userText, String modelResponse, ArrayList<String> intent) {
        String userId = getCurrentUserId();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("logs").push();

        ZonedDateTime dtNow = ZonedDateTime.now(ZoneId.of("GMT+7"));

        ArrayList<String> mainIntent = new ArrayList<>();
        List<String> taskIntents = List.of("Check", "Add", "Edit", "Remove");
        List<String> searchIntents = List.of("Search", "GoogleSearch");
        if (intent.stream().anyMatch(taskIntents::contains)) {
            mainIntent.add("Task");
        }
        if (intent.stream().anyMatch(searchIntents::contains)) {
            mainIntent.add("Search");
        }
        if(intent.contains("Plan")){
            mainIntent.add("Plan");
        }

        reference.setValueAsync(Map.of(
                "userId", userId,
                "userText", userText,
                "modelResponse", modelResponse,
                "timeStamp", dtNow.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                "intent", mainIntent
        ));
    }

    public CompletableFuture<List<Map<String, Object>>> getLogs(String intent, @Nullable String date, @Nullable String keyword){
        String userId = getCurrentUserId();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("logs");
        Query query = reference.orderByChild("userId").equalTo(userId);
        CompletableFuture<List<Map<String, Object>>> future = new CompletableFuture<>();
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Map<String, Object>> logs = new ArrayList<>();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Map<String, Object> logData = (Map<String, Object>) child.getValue();
                    if (logData != null) {
                        logData.put("id", child.getKey());
                        logs.add(logData);
                    }
                }
                List<Map<String, Object>> filteredLogs = logs.stream()
                        .filter(log -> {
                            Object intentValue = log.get("intent");
                            if (intentValue instanceof List) {
                                return ((List<?>) intentValue).contains(intent);
                            }
                            return false;
                        })
                        .toList();
                if(date != null){
                    filteredLogs = filteredLogs.stream()
                            .filter(log -> ((String) log.get("timeStamp")).startsWith(date))
                            .toList();
                }
                if(keyword != null){
                    filteredLogs = filteredLogs.stream()
                            .filter(log -> ((String) log.get("userText")).contains(keyword) ||
                                    ((String) log.get("modelResponse")).contains(keyword))
                            .toList();
                }
                future.complete(filteredLogs);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.obtrudeException(databaseError.toException());
            }
        });
        return future;
    }
}
