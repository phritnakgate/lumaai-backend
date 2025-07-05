package org.bkkz.lumabackend.config;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                .title("LumaBackEnd API")
                .description("nothing beats a jet2 holiday and right now you can save £50 per person. that's £200 off for a family of 4.")
                .version("1.0.0"));
    }
}
