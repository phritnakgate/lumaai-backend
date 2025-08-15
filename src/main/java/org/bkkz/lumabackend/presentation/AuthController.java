package org.bkkz.lumabackend.presentation;

import jakarta.validation.Valid;
import org.bkkz.lumabackend.model.*;
import org.bkkz.lumabackend.security.JwtUtil;
import org.bkkz.lumabackend.service.AuthService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${jwt.access-token.expiration.ms}")
    private long accessTokenExpirationMs;

    @NotNull
    private ResponseEntity<?> getOkResponseEntity(AuthResponse newToken) {
        Map<String, String> response = new HashMap<>();
        response.put("access_token", newToken.getAccessToken());
        response.put("token_type", "Bearer");
        response.put("expires_in", accessTokenExpirationMs / 1000 + "");
        response.put("refresh_token", newToken.getRefreshToken());
        return ResponseEntity.ok(response);
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
            AuthResponse jwtToken = authService.loginWithEmail(emailLoginRequest.getEmail(), emailLoginRequest.getPassword());
            return getOkResponseEntity(jwtToken);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_request"));
        }
    }

    @PostMapping("/login-google")
    public ResponseEntity<?> authenticateWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            AuthResponse jwtToken = authService.verifyGoogleIdTokenAndCreateSession(request.getIdToken());
            return getOkResponseEntity(jwtToken);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_request"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        try {
            AuthResponse newToken = authService.refreshAccessToken(refreshTokenRequest.getRefreshToken());
            return getOkResponseEntity(newToken);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "invalid_grant");
            errorResponse.put("error_description", "The refresh token is invalid or expired.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
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
