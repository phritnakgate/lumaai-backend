package org.bkkz.lumabackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.InputStream;
import java.util.Objects;

@Configuration
@Profile("local")
public class FirebaseLocalConfig {
    public void loadEnv() {
        try {
            Dotenv dotenv = Dotenv.load();
            System.setProperty("firebase.api-key", Objects.requireNonNull(dotenv.get("FIREBASE_API_KEY")));
            System.setProperty("firebase.database-url", Objects.requireNonNull(dotenv.get("FIREBASE_DATABASE_URL")));
            System.setProperty("firebase.bucket-name", Objects.requireNonNull(dotenv.get("FIREBASE_BUCKET_NAME")));
            System.setProperty("vertex-ai.api-key", Objects.requireNonNull(dotenv.get("VERTEX_AI_API_KEY")));
            System.setProperty("google.client-id", Objects.requireNonNull(dotenv.get("GOOGLE_CLIENT_ID")));
            System.setProperty("google.client-secret", Objects.requireNonNull(dotenv.get("GOOGLE_CLIENT_SECRET")));
            System.setProperty("spring.mail.username", Objects.requireNonNull(dotenv.get("SPRING_MAIL_USERNAME")));
            System.setProperty("spring.mail.password", Objects.requireNonNull(dotenv.get("SPRING_MAIL_PASSWORD")));
            System.out.println("✅ .env loaded");
        } catch (Exception e) {
            System.err.println("Could not load .env");
        }
    }

    @PostConstruct
    public void init() {
        loadEnv();
        try {
            InputStream serviceAccount = getClass()
                    .getClassLoader()
                    .getResourceAsStream("firebase-adminsdk.json");

            assert serviceAccount != null;
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(System.getProperty("firebase.database-url"))
                    .setStorageBucket(System.getProperty("firebase.bucket-name"))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("✅ Firebase initialized (LOCAL)");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
