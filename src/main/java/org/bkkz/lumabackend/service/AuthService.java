package org.bkkz.lumabackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.bkkz.lumabackend.model.AuthResponse;
import org.bkkz.lumabackend.model.Register;
import org.bkkz.lumabackend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    private final ObjectMapper objectMapper;
    @Value("${firebase.api-key}")
    private String firebaseApiKey;

    private final JwtUtil jwtUtil;

    public AuthService(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    HttpClient client = HttpClient.newHttpClient();

    public AuthResponse loginWithEmail(String email, String password) throws Exception {
        String authUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + firebaseApiKey;
        try{
            String body = """
            {
                "email": "%s",
                "password": "%s",
                "returnSecureToken": true
            }
            """.formatted(email, password);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(authUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
            if(response.statusCode() == 200){
                Map<String, String> firebaseResponse = objectMapper.readValue(response.body(), Map.class);
                String idToken = firebaseResponse.get("idToken");
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);

                String accessToken = jwtUtil.generateAccessToken(decodedToken.getUid(), decodedToken.getEmail());
                String refreshToken = jwtUtil.generateRefreshToken(decodedToken.getUid());

                return new AuthResponse(accessToken, refreshToken);
            }else{
                throw new RuntimeException("Login Failed! Invalid credentials.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Login Failed! Internal error: " + e.getMessage());
        }
    }

    public AuthResponse verifyGoogleIdTokenAndCreateSession(String idTokenString) throws FirebaseAuthException {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idTokenString);
        String uid = decodedToken.getUid();
        String name = decodedToken.getName();
        String email = decodedToken.getEmail();
        System.out.println("UID: " + uid + ", Name: " + name + ", Email: " + email);

        String accessToken = jwtUtil.generateAccessToken(uid, email);
        String refreshToken = jwtUtil.generateRefreshToken(uid);

        saveOrUpdateUser(uid, name, email);

        System.out.println("AccessToken: " + accessToken + "\nRefreshToken: " + refreshToken);
        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refreshAccessToken(String refreshToken) throws Exception {
        String uid;
        try {
            uid = jwtUtil.extractUid(refreshToken);
        } catch (Exception e) {
            throw new RuntimeException("Invalid refresh token.");
        }


        // Validate the provided token against the stored one
        if (!jwtUtil.validateToken(refreshToken, uid)) {
            throw new RuntimeException("Refresh token is invalid or expired. Please log in again.");
        }

        // If valid, issue a new access token
        UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
        String newAccessToken = jwtUtil.generateAccessToken(userRecord.getUid(), userRecord.getEmail());
        String newRefreshToken = jwtUtil.generateRefreshToken(userRecord.getUid());

        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    private void saveOrUpdateUser(String uid, String name, String email) {
        try {
            final FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference ref = database.getReference("users").child(uid);
            Map<String, Object> userData = new HashMap<>();
            userData.put("uid", uid);
            userData.put("name", name);
            userData.put("email", email);
            ref.updateChildrenAsync(userData);
            System.out.println("User data saved/updated for UID: " + uid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void registerUser(Register register) throws FirebaseAuthException {
        UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                .setEmail(register.getEmail())
                .setPassword(register.getPassword())
                .setDisplayName(register.getName());
        UserRecord userRecord = FirebaseAuth.getInstance().createUser(createRequest);
        saveOrUpdateUser(userRecord.getUid(), register.getName(), register.getEmail());
    }

    public void logout(String uid) throws FirebaseAuthException {
        FirebaseAuth.getInstance().revokeRefreshTokens(uid);
    }
}
