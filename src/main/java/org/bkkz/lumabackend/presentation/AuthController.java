package org.bkkz.lumabackend.presentation;

import com.google.firebase.auth.FirebaseAuthException;
import jakarta.validation.Valid;
import org.bkkz.lumabackend.model.EmailLoginRequest;
import org.bkkz.lumabackend.model.GoogleLoginRequest;
import org.bkkz.lumabackend.model.RefreshTokenRequest;
import org.bkkz.lumabackend.model.Register;
import org.bkkz.lumabackend.security.JwtUtil;
import org.bkkz.lumabackend.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }



    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Register register) {
        try{
            authService.registerUser(register);
           return ResponseEntity.ok(Map.of(
                   "result", "User registered successfully"
           ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login-email")
    public ResponseEntity<?> authenticateWithEmail(@RequestBody EmailLoginRequest emailLoginRequest){
        try{
            String jwtToken = authService.loginWithEmail(emailLoginRequest.getEmail(), emailLoginRequest.getPassword()).getAccessToken();
            Map<String, String> response = new HashMap<>();
            response.put("jwt", jwtToken);
            response.put("result", "Authentication successful");
            return ResponseEntity.ok(response);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred | " + e.getMessage());
        }
    }

    @PostMapping("/login-google")
    public ResponseEntity<?> authenticateWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            String jwtToken = authService.verifyGoogleIdTokenAndCreateSession(request.getIdToken()).getAccessToken();
            Map<String, String> response = new HashMap<>();
            response.put("jwt", jwtToken);
            response.put("result", "Authentication successful");
            return ResponseEntity.ok(response);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Google ID Token: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred.");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        try {
            String newAccessToken = authService.refreshAccessToken(refreshTokenRequest.getRefreshToken());
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Missing or invalid Authorization header");
        }
        try {
            String jwt = authHeader.substring(7);
            String uid = jwtUtil.extractUid(jwt);
            authService.logout(uid);
            return ResponseEntity.ok(Map.of("result", "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to revoke token: " + e.getMessage()));
        }
    }
}
