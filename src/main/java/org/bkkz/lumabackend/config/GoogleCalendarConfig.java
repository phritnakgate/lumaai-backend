package org.bkkz.lumabackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class GoogleCalendarConfig {
    @Configuration
    @Profile("!prod")
    static class DevConfig {
        @Value("${google.client-id}")
        private String clientId;

        @Value("${google.client-secret}")
        private String clientSecret;

        @Bean("googleClientId")
        public String getClientId() {
            return clientId;
        }
        @Bean("googleClientSecret")
        public String getClientSecret() {
            return clientSecret;
        }
    }

    @Configuration
    @Profile("prod")
    static class ProdConfig {
        @Bean("googleClientId")
        public String getClientId() {
            String clientId = System.getenv("GOOGLE_CLIENT_ID");
            if (clientId == null || clientId.isEmpty()) {
                throw new IllegalStateException("GOOGLE_CLIENT_ID environment variable not set for prod profile");
            }
            return clientId;
        }

        @Bean("googleClientSecret")
        public String getClientSecret() {
            String clientSecret = System.getenv("GOOGLE_CLIENT_SECRET");
            if (clientSecret == null || clientSecret.isEmpty()) {
                throw new IllegalStateException("GOOGLE_CLIENT_SECRET environment variable not set for prod profile");
            }
            return clientSecret;
        }
    }
}
