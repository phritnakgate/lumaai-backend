package org.bkkz.lumabackend.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bkkz.lumabackend.model.Login;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Profile("!prod")
@RestController
@RequestMapping("/api/test")

public class TestController {
    @Value("${firebase.api-key}")
    private String firebaseApiKey;

    HttpClient client = HttpClient.newHttpClient();

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Login login){

        if(login.getEmail().isEmpty() || login.getPassword().isEmpty()){
            return ResponseEntity.badRequest().body("Email or Password is invalid!");
        }else{
            String authUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + firebaseApiKey;
            try{
                String body = """
                {
                    "email": "%s",
                    "password": "%s",
                    "returnSecureToken": true
                }
                """.formatted(login.getEmail(), login.getPassword());
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(authUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println(response.body());
                if(response.statusCode() == 200){
                    return ResponseEntity.ok(new ObjectMapper().readValue(response.body(), Map.class).toString());
                }else{
                    return ResponseEntity.badRequest().body("Login Failed!");
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()).toString());
            }
        }
    }
}
