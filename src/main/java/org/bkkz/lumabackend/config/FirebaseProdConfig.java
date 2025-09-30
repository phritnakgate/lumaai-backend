package org.bkkz.lumabackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.FileInputStream;

@Configuration
@Profile("prod")
public class FirebaseProdConfig {
    @PostConstruct
    public void init() {
        try {
            FileInputStream serviceAccount = new FileInputStream("/secrets/firebase/firebase-adminsdk");

            String databaseUrl = System.getenv("DATABASE_URL");
            String bucketName = System.getenv("BUCKET_NAME");

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(databaseUrl)
                    .setStorageBucket(bucketName)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("✅ Firebase initialized (PROD)");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
