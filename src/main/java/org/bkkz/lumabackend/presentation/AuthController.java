package org.bkkz.lumabackend.presentation;

import org.bkkz.lumabackend.model.Register;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.google.firebase.auth.*;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Register register) {
        try{
            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setEmail(register.getEmail())
                    .setPassword(register.getPassword())
                    .setDisplayName(register.getName());
           UserRecord userRecord = FirebaseAuth.getInstance().createUser(createRequest);
           DatabaseReference reference = FirebaseDatabase.getInstance()
                   .getReference("users")
                   .child(userRecord.getUid());
           reference.setValueAsync(Map.of(
                   "id",userRecord.getUid(),
                   "email", userRecord.getEmail(),
                   "displayName", userRecord.getDisplayName()
           ));

           return ResponseEntity.ok(Map.of(
                   "message", "User registered successfully"
           ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Missing or invalid Authorization header");
        }

        String idToken = authHeader.substring(7);
        try {
            String uid = FirebaseAuth.getInstance().verifyIdToken(idToken).getUid();
            FirebaseAuth.getInstance().revokeRefreshTokens(uid);
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Failed to revoke token: " + e.getMessage()));
        }
    }
}
