package org.bkkz.lumabackend.presentation;

import jakarta.validation.Valid;
import org.bkkz.lumabackend.model.authentication.*;
import org.bkkz.lumabackend.security.JwtUtil;
import org.bkkz.lumabackend.service.AuthService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
            String authorizationCode = authService.registerUser(register);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "result", "User registered successfully",
                    "authorization_code", authorizationCode
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login-email")
    public ResponseEntity<?> authenticateWithEmail(@RequestBody EmailLoginRequest emailLoginRequest){
        if (!StringUtils.hasText(emailLoginRequest.getCodeChallenge()) || !"S256".equalsIgnoreCase(emailLoginRequest.getCodeChallengeMethod())) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_request", "error_description", "code_challenge and code_challenge_method (S256) are required."));
        }
        try{
            String authCode = authService.loginWithEmail(
                    emailLoginRequest.getEmail(),
                    emailLoginRequest.getPassword(),
                    emailLoginRequest.getCodeChallenge(),
                    emailLoginRequest.getCodeChallengeMethod()
            );
            return ResponseEntity.ok(Map.of("code", authCode));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_request"));
        }
    }

    @PostMapping("/token")
    public ResponseEntity<?> exchangeCodeForToken(@RequestBody TokenRequest tokenRequest) {
        String grantType = tokenRequest.getGrantType();

        if (!StringUtils.hasText(grantType)) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_request", "error_description", "grant_type is required."));
        }

        try {
            switch (grantType){
                case "authorization_code":
                    if (!StringUtils.hasText(tokenRequest.getCode()) || !StringUtils.hasText(tokenRequest.getCodeVerifier())) {
                        return ResponseEntity.badRequest().body(Map.of("error", "invalid_request", "error_description", "code and code_verifier are required for this grant type."));
                    }
                    AuthResponse tokens = authService.exchangeCodeForToken(tokenRequest.getCode(), tokenRequest.getCodeVerifier());
                    return getOkResponseEntity(tokens);
                case "refresh_token":
                    if (!StringUtils.hasText(tokenRequest.getRefreshToken())) {
                        return ResponseEntity.badRequest().body(Map.of("error", "invalid_request", "error_description", "refresh_token is required for this grant type."));
                    }
                    AuthResponse newToken = authService.refreshAccessToken(tokenRequest.getRefreshToken());
                    return getOkResponseEntity(newToken);
                default:
                    return ResponseEntity.badRequest().body(Map.of("error", "unsupported_grant_type"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid_grant", "error_description", e.getMessage()));
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

//    @PostMapping("/refresh")
//    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
//        try {
//            AuthResponse newToken = authService.refreshAccessToken(refreshTokenRequest.getRefreshToken());
//            return getOkResponseEntity(newToken);
//        } catch (Exception e) {
//            Map<String, String> errorResponse = new HashMap<>();
//            errorResponse.put("error", "invalid_grant");
//            errorResponse.put("error_description", "The refresh token is invalid or expired.");
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
//        }
//    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        if (refreshTokenRequest.getRefreshToken() == null || refreshTokenRequest.getRefreshToken().isEmpty()) {
            return ResponseEntity.badRequest().body("Missing or invalid Token");
        }
        try {
            String uid = jwtUtil.extractUid(refreshTokenRequest.getRefreshToken());
            authService.logout(uid);
            return ResponseEntity.ok(Map.of("result", "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to revoke token: " + e.getMessage()));
        }
    }
}
