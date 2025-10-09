package org.bkkz.lumabackend.presentation;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import org.bkkz.lumabackend.model.googleCalendar.ConnectCalendarRequest;
import org.bkkz.lumabackend.service.GoogleCalendarService;
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
            googleCalendarService.exchangeCodeAndStoreRefreshToken(connectCalendarRequest.getAuthCode());
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

}
