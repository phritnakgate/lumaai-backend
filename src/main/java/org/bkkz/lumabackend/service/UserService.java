package org.bkkz.lumabackend.service;

import com.google.firebase.database.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class UserService {
    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public CompletableFuture<ResponseEntity<?>> getCurrentUserData() {
        String userId = getCurrentUserId();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users").child(userId);
        CompletableFuture<ResponseEntity<?>> future = new CompletableFuture<>();

        reference.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Map<String, Object>> userData = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    Map<String, Object> userMap = (Map<String, Object>) dataSnapshot.getValue();
                    userData.add(userMap);
                    future.complete(ResponseEntity.ok().body(Map.of("results", userData)));
                } else {
                    future.complete(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "User not found.")));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.complete(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", databaseError.getMessage())));
            }
        });
        return future;
    }
}
