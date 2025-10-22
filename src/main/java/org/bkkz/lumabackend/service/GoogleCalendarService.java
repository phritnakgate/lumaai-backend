package org.bkkz.lumabackend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import com.google.firebase.database.*;
import org.bkkz.lumabackend.model.googleCalendar.CalendarEventRequest;
import org.bkkz.lumabackend.model.task.CreateTaskRequest;
import org.bkkz.lumabackend.model.task.UpdateTaskRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class GoogleCalendarService {

    private final TaskService taskService;
    private final String clientId;
    private final String clientSecret;

    public GoogleCalendarService(
            TaskService taskService,
            @Qualifier("googleClientId") String clientId,
            @Qualifier("googleClientSecret") String clientSecret
    ) {
        this.taskService = taskService;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    private final NetHttpTransport httpTransport = new NetHttpTransport();
    private final GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
    private final ZoneId zoneId = ZoneId.of("GMT+7");

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public void exchangeCodeAndStoreRefreshToken(String authCode, String email) throws IOException, GeneralSecurityException {
        String userId = getCurrentUserId();
        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                httpTransport, jsonFactory, clientId, clientSecret, authCode, "https://developers.google.com/oauthplayground"
        ).execute();

        String refreshToken = tokenResponse.getRefreshToken();
        String verifiedEmail = email;
        if (email.isEmpty()) {
            String idTokenString = tokenResponse.getIdToken();
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(httpTransport, jsonFactory)
                    .setAudience(Collections.singletonList(clientId))
                    .build();
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new GeneralSecurityException("ID Token verification failed.");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            verifiedEmail = payload.getEmail();
        }


        if (refreshToken != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            userRef.child("googleRefreshToken").setValueAsync(refreshToken);
            userRef.child("googleCalendarEmail").setValueAsync(verifiedEmail);
            System.out.println("Stored Refresh Token for user: " + userId);
        }
    }

    private CompletableFuture<String> getRefreshTokenFromFirebaseAsync() {
        CompletableFuture<String> future = new CompletableFuture<>();
        String userId = getCurrentUserId();
        DatabaseReference tokenRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("googleRefreshToken");

        tokenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String token = dataSnapshot.getValue(String.class);
                if (token != null && !token.isEmpty()) {
                    future.complete(token);
                } else {
                    future.completeExceptionally(new IOException("Refresh token not found for user: " + userId));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });

        return future;
    }

    public CompletableFuture<Boolean> checkConnectionStatusAsync() {
        return getRefreshTokenFromFirebaseAsync()
                .thenApply(token -> token != null && !token.isEmpty())
                .exceptionally(ex -> false);
    }

    private CompletableFuture<List<Event>> getAllCalendarEvents() {
        CompletableFuture<List<Event>> future = new CompletableFuture<>();
        getRefreshTokenFromFirebaseAsync()
                .thenApply(refreshToken -> {
                    UserCredentials credentials = UserCredentials.newBuilder()
                            .setClientId(clientId)
                            .setClientSecret(clientSecret)
                            .setRefreshToken(refreshToken)
                            .build();

                    Calendar c = new Calendar.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(credentials))
                            .setApplicationName("LumaApp")
                            .build();

                    try {
                        return c.events().list("primary")
                                .setOrderBy("startTime")
                                .setSingleEvents(true)
                                .execute()
                                .getItems();
                    } catch (Exception e) {
                        return null;
                    }

                }).thenCompose(events -> {
                    if (events != null) {
                        System.out.println("Found " + events.size() + " events");
                        future.complete(events);
                    } else {
                        future.completeExceptionally(new IOException("Failed to fetch events from Google Calendar"));
                    }
                    return future;
                });
        return future;
    }

    public CompletableFuture<Map<String, Integer>> syncGoogleCalendar() {
        String userId = getCurrentUserId();
        CompletableFuture<List<Event>> calenderEventsFuture = getAllCalendarEvents();
        CompletableFuture<Map<String, DataSnapshot>> firebaseTasksFuture = getGoogleTasksFromFirebaseAsync();

        return calenderEventsFuture.thenCombine(firebaseTasksFuture, (events, firebaseTasks) -> {

            AtomicInteger created = new AtomicInteger();
            AtomicInteger updated = new AtomicInteger();
            AtomicInteger deleted = new AtomicInteger();

            Set<String> firebaseTaskIds = firebaseTasks.keySet();
            Map<String, Event> googleEventMap = events.stream()
                    .collect(Collectors.toMap(Event::getId, event -> event));
            Set<String> googleEventIds = googleEventMap.keySet();
            System.out.println("Google Events: " + googleEventIds + ", Firebase Google Tasks: " + firebaseTaskIds);

            // 1. สร้างงานใหม่ใน Firebase สำหรับเหตุการณ์ Google ที่ไม่มีใน Firebase
            Set<String> eventsToCreate = googleEventIds.stream()
                    .filter(id -> !firebaseTaskIds.contains(id))
                    .collect(Collectors.toSet());
            System.out.println("eventsToCreate: " + eventsToCreate);
            for (String eventId : eventsToCreate) {
                Event event = googleEventMap.get(eventId);
                CreateTaskRequest newTask = convertEventToCreateTaskRequest(event);
                taskService.createTask(newTask, true, eventId, userId);
                created.getAndIncrement();
            }


            // 2. อัปเดตงานใน Firebase สำหรับเหตุการณ์ Google ที่มีอยู่แล้วแต่มีการเปลี่ยนแปลง
            Set<String> eventsToCheckForUpdate = googleEventIds.stream()
                    .filter(firebaseTaskIds::contains)
                    .collect(Collectors.toSet());
            System.out.println("eventsToCheckForUpdate: " + eventsToCheckForUpdate);
            for (String eventId : eventsToCheckForUpdate) {
                Event googleEvent = googleEventMap.get(eventId);
                DataSnapshot firebaseTaskSnapshot = firebaseTasks.get(eventId);
                if (isEventModified(googleEvent, firebaseTaskSnapshot)) {
                    UpdateTaskRequest updatedTask = convertEventToUpdateTaskRequest(googleEvent, firebaseTaskSnapshot);
                    taskService.updateTask(eventId, updatedTask, userId);
                    updated.getAndIncrement();
                }
            }

            // 3. ลบงานใน Firebase ที่ไม่มีเหตุการณ์ Google ที่เกี่ยวข้องอีกต่อไป
            Set<String> tasksToDelete = firebaseTaskIds.stream()
                    .filter(id -> !googleEventIds.contains(id))
                    .collect(Collectors.toSet());
            System.out.println("tasksToDelete: " + tasksToDelete);
            for (String taskId : tasksToDelete) {
                taskService.deleteTask(taskId, userId);
                deleted.getAndIncrement();
            }
            return Map.of(
                    "created", created.get(),
                    "updated", updated.get(),
                    "deleted", deleted.get()
            );
        });
    }

    private CompletableFuture<Map<String, DataSnapshot>> getGoogleTasksFromFirebaseAsync() {
        String userId = getCurrentUserId();
        CompletableFuture<Map<String, DataSnapshot>> future = new CompletableFuture<>();
        DatabaseReference tasksRef = FirebaseDatabase.getInstance().getReference("tasks");
        Query query = tasksRef.orderByChild("userId").equalTo(userId);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, DataSnapshot> googleTasks = new HashMap<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Boolean isGoogleTask = snapshot.child("isGoogleCalendarTask").getValue(Boolean.class);
                    if (Boolean.TRUE.equals(isGoogleTask)) {
                        googleTasks.put(snapshot.getKey(), snapshot);
                    }
                }
                System.out.println("Fetched " + googleTasks.size() + " Google Calendar tasks from Firebase for user: " + userId);
                future.complete(googleTasks);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }

    private CreateTaskRequest convertEventToCreateTaskRequest(Event event) {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setName(event.getSummary());
        request.setDescription(event.getDescription());

        EventDateTime start = event.getStart();
        if (start.getDateTime() != null) { // Event ปกติ
            ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(start.getDateTime().getValue()), zoneId);
            request.setDueDate(zdt.toLocalDate().toString());
            request.setDueTime(zdt.toLocalTime().toString());
        } else { // All-day Event
            request.setDueDate(start.getDate().toStringRfc3339());
            request.setDueTime("");
        }
        String eventName = request.getName().toLowerCase(Locale.ENGLISH);
        if (eventName.contains("โค้ด") || eventName.contains("code") || eventName.contains("เขียน") || eventName.contains("coding")) {
            request.setCategory(0);
        } else if (eventName.contains("ประชุม") || eventName.contains("คุยงาน") || eventName.contains("meeting")) {
            request.setCategory(1);
        } else if (eventName.contains("อบรม") || eventName.contains("training") || eventName.contains("course")) {
            request.setCategory(2);
        } else if (eventName.contains("poc") || eventName.contains("ลอง") || eventName.contains("ค้น")) {
            request.setCategory(3);
        } else {
            request.setCategory(4);
        }
        request.setPriority(0);

        return request;
    }

    private UpdateTaskRequest convertEventToUpdateTaskRequest(Event event, DataSnapshot firebaseTask) {
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setName(event.getSummary());
        request.setDescription(event.getDescription());

        String firebaseDateTimeStr = firebaseTask.child("dateTime").getValue(String.class);
        ZonedDateTime originalFirebaseZdt = null;
        if (firebaseDateTimeStr != null) {
            originalFirebaseZdt = ZonedDateTime.parse(firebaseDateTimeStr);
        }
        LocalDate newGoogleDate;
        EventDateTime start = event.getStart();

        if (start.getDateTime() != null) {
            ZonedDateTime googleZdt = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(start.getDateTime().getValue()),
                    zoneId
            );
            newGoogleDate = googleZdt.toLocalDate();
        } else {
            newGoogleDate = LocalDate.parse(start.getDate().toStringRfc3339());
        }
        ZonedDateTime finalZdt;
        if (originalFirebaseZdt != null) {
            finalZdt = originalFirebaseZdt
                    .withYear(newGoogleDate.getYear())
                    .withMonth(newGoogleDate.getMonthValue())
                    .withDayOfMonth(newGoogleDate.getDayOfMonth());
        } else {
            finalZdt = newGoogleDate.atStartOfDay(zoneId);
        }
        request.setDateTime(finalZdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return request;
    }

    private boolean isEventModified(Event googleEvent, DataSnapshot firebaseTask) {
        String googleName = googleEvent.getSummary();
        String firebaseName = firebaseTask.child("name").getValue(String.class);
        if (!Objects.equals(googleName, firebaseName)) return true;

        String googleDescription = googleEvent.getDescription();
        String firebaseDescription = firebaseTask.child("description").getValue(String.class);
        if (!Objects.equals(googleDescription, firebaseDescription)) return true;

        String firebaseDateTimeStr = firebaseTask.child("dateTime").getValue(String.class);
        if (firebaseDateTimeStr == null) return true;

        LocalDate firebaseDate = ZonedDateTime.parse(firebaseDateTimeStr).toLocalDate();
        LocalDate googleDate;
        EventDateTime start = googleEvent.getStart();

        if (start.getDateTime() != null) {
            ZonedDateTime googleZdt = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(start.getDateTime().getValue()),
                    zoneId
            );
            googleDate = googleZdt.toLocalDate();
        } else {
            googleDate = LocalDate.parse(start.getDate().toStringRfc3339());
        }
        return !firebaseDate.equals(googleDate);
    }

    public void revokeGoogleCalendar() {
        String userId = getCurrentUserId();
        deleteGoogleRefreshTokenAsync(userId).thenCompose(v ->
                deleteGoogleCalendarTasksAsync(userId)
        );
    }

    public CompletableFuture<Void> deleteGoogleRefreshTokenAsync(String userId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        DatabaseReference tokenRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("googleRefreshToken");


        tokenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue() != null) {
                    String refreshToken = snapshot.getValue(String.class);
                    if (refreshToken == null || refreshToken.isEmpty()) {
                        future.completeExceptionally(new Exception("refresh token ว่าง"));
                        return;
                    }

                    // revoke ก่อน แล้วค่อยลบจาก Firebase
                    revokeGoogleToken(refreshToken)
                            .thenRun(() -> tokenRef.removeValue((databaseError, databaseReference) -> {
                                if (databaseError == null) {
                                    future.complete(null);
                                } else {
                                    future.completeExceptionally(databaseError.toException());
                                }
                            }))
                            .exceptionally(ex -> {
                                // ถ้า revoke fail ก็ส่ง exception กลับ
                                future.completeExceptionally(ex);
                                return null;
                            });

                } else {
                    future.completeExceptionally(new Exception("ไม่พบ refresh token ใน Firebase"));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(error.toException());
            }
        });

        return future;

    }

    private CompletableFuture<Void> revokeGoogleToken(String refreshToken) {
        return CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL("https://oauth2.googleapis.com/revoke");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String params = "token=" + refreshToken;
                conn.getOutputStream().write(params.getBytes(StandardCharsets.UTF_8));

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    throw new IOException("Failed to revoke token, response code: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }


    private CompletableFuture<Void> deleteGoogleCalendarTasksAsync(String userId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        DatabaseReference tasksRef = FirebaseDatabase.getInstance().getReference("tasks");

        Query userTasksQuery = tasksRef.orderByChild("userId").equalTo(userId);

        userTasksQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    System.out.println("No tasks found for user: " + userId);
                    future.complete(null); // ไม่มี Task ของ user คนนี้ ก็ถือว่าสำเร็จ
                    return;
                }

                Map<String, Object> tasksToDelete = new HashMap<>();
                for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                    Boolean isGoogleTask = taskSnapshot.child("isGoogleCalendarTask").getValue(Boolean.class);

                    if (Boolean.TRUE.equals(isGoogleTask)) {
                        tasksToDelete.put(taskSnapshot.getKey(), null);
                    }
                }

                if (tasksToDelete.isEmpty()) {
                    System.out.println("No Google Calendar tasks to delete for user: " + userId);
                    future.complete(null);
                    return;
                }

                tasksRef.updateChildren(tasksToDelete, (databaseError, databaseReference) -> {
                    if (databaseError == null) {
                        System.out.println("Successfully deleted " + tasksToDelete.size() + " Google Calendar tasks for user: " + userId);
                        future.complete(null);
                    } else {
                        future.completeExceptionally(databaseError.toException());
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }

    public CompletableFuture<String> createGoogleCalendarEvent(CalendarEventRequest calendarEventRequest) {
        String userId = getCurrentUserId();
        CompletableFuture<String> future = new CompletableFuture<>();
        getRefreshTokenFromFirebaseAsync().thenApply(token -> {
                    UserCredentials credentials = UserCredentials.newBuilder()
                            .setClientId(clientId)
                            .setClientSecret(clientSecret)
                            .setRefreshToken(token)
                            .build();

                    Calendar c = new Calendar.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(credentials))
                            .setApplicationName("LumaApp")
                            .build();
                    String calendarId = "primary";
                    Event event = new Event()
                            .setSummary(calendarEventRequest.getName())
                            .setDescription(calendarEventRequest.getDescription());
                    EventDateTime start = new EventDateTime()
                            .setDate(new DateTime(calendarEventRequest.getStartTime()));
                    event.setStart(start);
                    EventDateTime end = new EventDateTime()
                            .setDate(new DateTime(calendarEventRequest.getEndTime()));
                    event.setEnd(end);

                    try {
                        Event createdEvent = c.events().insert(calendarId, event).execute();
                        CreateTaskRequest newTask = convertToCreateTaskReq(calendarEventRequest);
                        taskService.createTask(newTask, true, createdEvent.getId(), userId);
                        return future.complete(createdEvent.getId());
                    } catch (Exception e) {
                        return future.completeExceptionally(e);
                    }
                }
        );
        return future;
    }

    public void editGoogleCalendarEvent(String eventId, CalendarEventRequest calendarEventRequest) {
        String userId = getCurrentUserId();
        CompletableFuture<Void> future = new CompletableFuture<>();
        getRefreshTokenFromFirebaseAsync().thenApply(token -> {
                    UserCredentials credentials = UserCredentials.newBuilder()
                            .setClientId(clientId)
                            .setClientSecret(clientSecret)
                            .setRefreshToken(token)
                            .build();

                    Calendar c = new Calendar.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(credentials))
                            .setApplicationName("LumaApp")
                            .build();
                    String calendarId = "primary";
                    try {
                        Event event = c.events().get(calendarId, eventId).execute();
                        event.setSummary(calendarEventRequest.getName());
                        event.setDescription(calendarEventRequest.getDescription());
                        EventDateTime start = new EventDateTime()
                                .setDate(new DateTime(calendarEventRequest.getStartTime()));
                        event.setStart(start);
                        EventDateTime end = new EventDateTime()
                                .setDate(new DateTime(calendarEventRequest.getEndTime()));
                        event.setEnd(end);

                        c.events().patch(calendarId, eventId, event).execute();
                        UpdateTaskRequest updateTaskRequest = new UpdateTaskRequest();
                        updateTaskRequest.setName(calendarEventRequest.getName());
                        updateTaskRequest.setDescription(calendarEventRequest.getDescription());
                        ZonedDateTime taskDate = LocalDate.parse(calendarEventRequest.getStartTime())
                                .atTime(LocalTime.parse(calendarEventRequest.getAppTaskTime()))
                                .atZone(ZoneId.of("Asia/Bangkok"));
                        updateTaskRequest.setDateTime(taskDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                        updateTaskRequest.setCategory(calendarEventRequest.getAppCategory());
                        updateTaskRequest.setPriority(calendarEventRequest.getAppPriority());
                        System.out.println("Updating task with name" +updateTaskRequest.getName() + " with dateTime " + updateTaskRequest.getDateTime());
                        taskService.updateTask(eventId, updateTaskRequest, userId);
                        return future.complete(null);
                    } catch (Exception e) {
                        return future.completeExceptionally(e);
                    }
                }
        );
    }

    public void deleteGoogleCalendarEvent(String eventId) {
        String userId = getCurrentUserId();
        CompletableFuture<Void> future = new CompletableFuture<>();
        getRefreshTokenFromFirebaseAsync().thenApply(token -> {
                    UserCredentials credentials = UserCredentials.newBuilder()
                            .setClientId(clientId)
                            .setClientSecret(clientSecret)
                            .setRefreshToken(token)
                            .build();

                    Calendar c = new Calendar.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(credentials))
                            .setApplicationName("LumaApp")
                            .build();
                    String calendarId = "primary";
                    try {
                        c.events().delete(calendarId, eventId).execute();
                        taskService.deleteTask(eventId, userId);
                        return future.complete(null);
                    } catch (Exception e) {
                        return future.completeExceptionally(e);
                    }
                }
        );
    }

    @NotNull
    private static CreateTaskRequest convertToCreateTaskReq(CalendarEventRequest calendarEventRequest) {
        CreateTaskRequest newTask = new CreateTaskRequest();
        newTask.setName(calendarEventRequest.getName());
        newTask.setDescription(calendarEventRequest.getDescription());
        newTask.setDueDate(calendarEventRequest.getStartTime());
        newTask.setDueTime("");
        newTask.setCategory(4);
        newTask.setPriority(0);
        return newTask;
    }
}
