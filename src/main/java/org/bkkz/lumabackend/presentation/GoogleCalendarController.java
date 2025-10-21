package org.bkkz.lumabackend.presentation;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import org.bkkz.lumabackend.model.googleCalendar.ConnectCalendarRequest;
import org.bkkz.lumabackend.model.googleCalendar.CreateCalendarEventRequest;
import org.bkkz.lumabackend.service.GoogleCalendarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/google-calendar")
public class GoogleCalendarController {

    private final GoogleCalendarService googleCalendarService;
    public GoogleCalendarController(GoogleCalendarService googleCalendarService) {
        this.googleCalendarService = googleCalendarService;
    }


    @PostMapping("/auth")
    public ResponseEntity<?> connect(@RequestBody ConnectCalendarRequest connectCalendarRequest) {
        try{
            googleCalendarService.exchangeCodeAndStoreRefreshToken(connectCalendarRequest.getAuthCode(), connectCalendarRequest.getEmail());
            return ResponseEntity.ok().body(Map.of("result","Successfully connected Google Calendar."));
        }catch (GoogleJsonResponseException e) {
            System.err.println("Google API Error: " + e.getDetails());
            return ResponseEntity.badRequest().body(Map.of("error", e.getDetails()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/sync")
    public ResponseEntity<?> sync() {
        try{
            Map<String, Integer> result = googleCalendarService.syncGoogleCalendar().get();
            return ResponseEntity.ok().body(Map.of(
                    "result","Successfully synced events from Google Calendar.",
                    "results", result)
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/connection")
    public ResponseEntity<Map<String, Object>> checkConnection() {
        try{
            boolean isConnected = googleCalendarService.checkConnectionStatusAsync().get();
            if(isConnected){
                return ResponseEntity.ok().body(Map.of(
                        "result", "true")
                );
            }else{
                return ResponseEntity.ok().body(Map.of(
                        "result", "false")
                );
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }
    @DeleteMapping("/connection")
    public ResponseEntity<Map<String, Object>> revokeConnection() {
        try{
            googleCalendarService.revokeGoogleCalendar();
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(Map.of("result", "Successfully revoked Google Calendar connection."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/event")
    public ResponseEntity<?> createCalendarEvent(@RequestBody CreateCalendarEventRequest createCalendarEventRequest) {
        try {
            String result = googleCalendarService.createGoogleCalendarEvent(createCalendarEventRequest).get();
            return ResponseEntity.ok().body(Map.of("result", result));

        }catch (Exception e){
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

}
