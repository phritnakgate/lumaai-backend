package org.bkkz.lumabackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.bkkz.lumabackend.model.authentication.AuthResponse;
import org.bkkz.lumabackend.model.authentication.PkceDetails;
import org.bkkz.lumabackend.model.authentication.Register;
import org.bkkz.lumabackend.security.JwtUtil;
import org.bkkz.lumabackend.security.PkceUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {
    private final ObjectMapper objectMapper;
    @Value("${firebase.api-key}")
    private String firebaseApiKey;

    private final JwtUtil jwtUtil;

    private final Map<String, PkceDetails> authCodes = new ConcurrentHashMap<>();
    private final JavaMailSender mailSender;

    public AuthService(JwtUtil jwtUtil, ObjectMapper objectMapper, JavaMailSender mailSender) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.mailSender = mailSender;
    }

    HttpClient client = HttpClient.newHttpClient();

    private String generateAuthorizationCode(String uid, String email, String codeChallenge, String codeChallengeMethod) {
        String code = UUID.randomUUID().toString();
        PkceDetails details = new PkceDetails(uid, email, codeChallenge, codeChallengeMethod, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));
        authCodes.put(code, details);
        System.out.println("Generated Authorization Code: " + authCodes);
        return code;
    }

    public AuthResponse exchangeCodeForToken(String code, String codeVerifier) throws Exception {
        PkceDetails details = authCodes.get(code);

        if (details == null) {
            throw new RuntimeException("Invalid or expired authorization code.");
        }

        // Remove the code immediately to prevent reuse (single-use)
        authCodes.remove(code);

        if (System.currentTimeMillis() > details.getExpiryTime()) {
            throw new RuntimeException("Authorization code has expired.");
        }

        // Verify the PKCE code challenge
        if (!PkceUtil.verifyCodeChallenge(codeVerifier, details.getCodeChallenge(), details.getCodeChallengeMethod())) {
            throw new RuntimeException("Invalid code_verifier.");
        }

        // PKCE verification successful, generate tokens
        String accessToken = jwtUtil.generateAccessToken(details.getUid(), details.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(details.getUid());

        return new AuthResponse(accessToken, refreshToken);
    }

    public String loginWithEmail(String email, String password, String codeChallenge, String codeChallengeMethod) throws Exception {
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

                return generateAuthorizationCode(decodedToken.getUid(), decodedToken.getEmail(), codeChallenge, codeChallengeMethod);
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

        saveOrUpdateUser(uid, name, email, 1);

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

        // Validate the refresh token against Firebase
        UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
        Date tokenIssuedAt = jwtUtil.extractIssuedAt(refreshToken);
        long revocationTimestamp = userRecord.getTokensValidAfterTimestamp();
        if (tokenIssuedAt.getTime() <= revocationTimestamp) {
            throw new RuntimeException("Refresh token has been revoked. Please log in again.");
        }

        // If valid, issue a new access token
        String newAccessToken = jwtUtil.generateAccessToken(userRecord.getUid(), userRecord.getEmail());
        String newRefreshToken = jwtUtil.generateRefreshToken(userRecord.getUid());
        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    private void saveOrUpdateUser(String uid, String name, String email, int provider) {
        try {
            final FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference ref = database.getReference("users").child(uid);
            Map<String, Object> userData = new HashMap<>();
            userData.put("displayName", name);
            userData.put("email", email);
            userData.put("provider",provider);
            ref.updateChildrenAsync(userData);
            System.out.println("User data saved/updated for UID: " + uid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String registerUser(Register register) throws FirebaseAuthException {
        UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                .setEmail(register.getEmail())
                .setPassword(register.getPassword())
                .setDisplayName(register.getName());
        UserRecord userRecord = FirebaseAuth.getInstance().createUser(createRequest);
        saveOrUpdateUser(userRecord.getUid(), register.getName(), register.getEmail(), 0);
        return generateAuthorizationCode(
                userRecord.getUid(),
                userRecord.getEmail(),
                register.getCodeChallenge(),
                register.getCodeChallengeMethod()
        );
    }

    public void logout(String uid) throws FirebaseAuthException {
        FirebaseAuth.getInstance().revokeRefreshTokens(uid);
    }

    public void forgetPassword(String email) throws Exception {
        String link = FirebaseAuth.getInstance().generatePasswordResetLink(email);
        System.out.println("Password reset link: " + link);
        if(link == null || link.isEmpty()){
            throw new RuntimeException("Generate password reset link failed.");
        }else{
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("LUMA AI | Password Reset Request");
            message.setText("Dear User,\n\n" +
                    "We received a request to reset your password. Please click the link below to set a new password:\n\n" +
                    link + "\n\n" +
                    "If you did not request a password reset, please ignore this email.\n\n" +
                    "Best regards,\n" +
                    "LUMA AI Team");
            mailSender.send(message);
        }

    }
}
