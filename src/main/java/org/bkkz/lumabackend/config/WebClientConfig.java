package org.bkkz.lumabackend.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient llmWebClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8080/api/test") // Change to real LLM service URL later
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    @Profile("!prod")
    public Client vertexAIClientDev(){
        return Client.builder()
                .vertexAI(true)
                .apiKey(System.getProperty("vertex-ai.api-key"))
                .httpOptions(HttpOptions.builder().apiVersion("v1").build())
                .build();
    }
    @Bean
    @Profile("prod")
    public Client vertexAIClientProd(){
        return Client.builder()
                .vertexAI(true)
                .apiKey(System.getenv("vertex-ai.api-key"))
                .httpOptions(HttpOptions.builder().apiVersion("v1").build())
                .build();
    }
}
