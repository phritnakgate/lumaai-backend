package org.bkkz.lumabackend.service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
}
