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
